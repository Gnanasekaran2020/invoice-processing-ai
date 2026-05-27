package com.invoice.service;

import com.invoice.domain.entity.Invoice;
import com.invoice.domain.entity.InvoiceDetail;
import com.invoice.domain.entity.User;
import com.invoice.domain.enums.InvoiceStatus;
import com.invoice.domain.enums.ProcessingStatus;
import com.invoice.domain.enums.UserRole;
import com.invoice.dto.request.InvoiceUploadRequest;
import com.invoice.dto.request.TextractCallbackRequest;
import com.invoice.dto.request.UpdateInvoiceRequest;
import com.invoice.dto.response.InvoiceDetailResponse;
import com.invoice.dto.response.InvoiceResponse;
import com.invoice.dto.response.MonthlyTrendResponse;
import com.invoice.exception.InvoiceNotFoundException;
import com.invoice.repository.InvoiceRepository;
import com.invoice.repository.UserRepository;
import com.invoice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/tiff", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L;
    private static final BigDecimal CONFIDENCE_THRESHOLD = BigDecimal.valueOf(70);

    // Internal identity used when LambdaTextractService generates the JWT
    private static final String LAMBDA_SERVICE_IDENTITY = "lambda-service@internal";

    @Value("${app.extraction.callback-secret}")
    private String configuredCallbackSecret;

    private final InvoiceRepository        invoiceRepository;
    private final UserRepository           userRepository;
    private final FileStorageService       fileStorageService;
    private final LambdaTextractService    lambdaTextractService;
    private final EmailNotificationService emailNotificationService;
    private final JwtTokenProvider         jwtTokenProvider;

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse uploadInvoice(InvoiceUploadRequest request, String email) {
        MultipartFile file = request.getFile();
        validateFile(file);

        User user = findUserByEmail(email);
        String objectKey = fileStorageService.uploadFile(file, "invoices/" + email);

        Invoice invoice = Invoice.builder()
                .user(user)
                .originalFileName(file.getOriginalFilename())
                .fileType(resolveFileType(file.getContentType()))
                .fileSizeBytes(file.getSize())
                .storagePath(objectKey)
                .storageBucket(fileStorageService.getBucket())
                .status(InvoiceStatus.PENDING)
                .processingStatus(ProcessingStatus.UPLOADED)
                .comments(request.getNotes())
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice uploaded: id={}, file={}", invoice.getInvoiceId(), file.getOriginalFilename());

        // Send upload confirmation email via SES
        emailNotificationService.sendInvoiceUploaded(invoice);

        // Trigger Lambda+Textract asynchronously — does not block the upload response
        triggerExtractionAsync(invoice.getInvoiceId(),
                fileStorageService.getBucket(), objectKey);
        return toResponse(invoice);
    }

    // ── Async Lambda trigger ──────────────────────────────────────────────────

    /**
     * Sets the invoice status to EXTRACTING then invokes the Textract Lambda
     * (fire-and-forget).  Runs in a Spring @Async thread so the upload API
     * call returns immediately to the client.
     */
    @Async
    public void triggerExtractionAsync(Integer invoiceId, String s3Bucket, String s3Key) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId.toString()));
        try {
            invoice.setProcessingStatus(ProcessingStatus.EXTRACTING);
            invoiceRepository.save(invoice);

            // Notify user that extraction has started
            emailNotificationService.sendInvoiceExtracting(invoice);

            lambdaTextractService.invokeLambda(invoiceId, s3Bucket, s3Key);
            log.info("Textract Lambda triggered for invoice: {}", invoiceId);
        } catch (Exception ex) {
            log.error("Failed to invoke Lambda for invoice {}: {}", invoiceId, ex.getMessage(), ex);
            invoice.setProcessingStatus(ProcessingStatus.FAILED);
            invoice.setProcessingError("Lambda invocation failed: " + ex.getMessage());
            invoiceRepository.save(invoice);

            // Notify user of failure
            emailNotificationService.sendProcessingFailed(invoice);
        }
    }

    // ── Textract callback (called by Lambda via POST) ──────────────────────────

    /**
     * Receives the Textract extraction result posted back by the Lambda function.
     *
     * Auth flow:
     *  1. LambdaTextractService generates a real signed JWT for "lambda-service@internal"
     *     and passes it to Lambda as callbackToken = "Bearer <jwt>".
     *  2. Lambda includes it in the callback request as:  Authorization: Bearer <jwt>
     *  3. JwtAuthenticationFilter is bypassed for this path (shouldNotFilter).
     *  4. We validate the JWT signature + identity here manually — no DB user lookup needed.
     */
    @Transactional
    public void handleExtractionCallback(Integer invoiceId, String authorizationHeader,
                                         TextractCallbackRequest payload) {
        // Step 1 — must be a Bearer token
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Extraction callback invoice={} — missing/malformed Authorization header", invoiceId);
            throw new AccessDeniedException("Missing or malformed Authorization header for extraction callback");
        }

        // Step 2 — validate JWT signature (uses the same app.jwt.secret)
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Extraction callback invoice={} — invalid JWT", invoiceId);
            throw new AccessDeniedException("Invalid JWT token in extraction callback");
        }

        // Step 3 — confirm caller identity is exactly the lambda service identity
        String callerIdentity = jwtTokenProvider.getEmailFromToken(token);
        if (!LAMBDA_SERVICE_IDENTITY.equals(callerIdentity)) {
            log.warn("Extraction callback invoice={} — unexpected identity: {}", invoiceId, callerIdentity);
            throw new AccessDeniedException("Unauthorized caller identity: " + callerIdentity);
        }

        log.info("Extraction callback JWT validated for invoice={} caller={}", invoiceId, callerIdentity);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId.toString()));

        long start = System.currentTimeMillis();
        try {
            invoice.setProcessingStatus(ProcessingStatus.AI_PROCESSING);

            applyExtractionResult(invoice, payload);

            long duration = payload.getProcessingDurationMs() != null
                    ? payload.getProcessingDurationMs()
                    : System.currentTimeMillis() - start;
            invoice.setProcessingDurationMs(duration);
            invoice.setAiModelUsed("aws-textract");

            BigDecimal confidence = payload.getConfidenceScore() != null
                    ? BigDecimal.valueOf(payload.getConfidenceScore())
                    : BigDecimal.ZERO;
            invoice.setAiConfidenceScore(confidence);

            // Skip COMPLETED/MANUAL_REVIEW if duplicate was detected during validation
            if (invoice.getStatus() != InvoiceStatus.DUPLICATE) {
                if (confidence.compareTo(CONFIDENCE_THRESHOLD) < 0) {
                    invoice.setProcessingStatus(ProcessingStatus.MANUAL_REVIEW);
                    emailNotificationService.sendManualReviewRequired(invoice);
                } else {
                    invoice.setProcessingStatus(ProcessingStatus.COMPLETED);
                    emailNotificationService.sendProcessingCompleted(invoice);
                }
            } else {
                invoice.setProcessingStatus(ProcessingStatus.COMPLETED);
            }

            invoiceRepository.save(invoice);
            log.info("Textract result applied for invoice={} in {}ms",
                    invoiceId, System.currentTimeMillis() - start);

        } catch (Exception ex) {
            log.error("Failed to apply Textract result for invoice={}: {}", invoiceId, ex.getMessage(), ex);
            invoice.setProcessingStatus(ProcessingStatus.FAILED);
            invoice.setProcessingError(ex.getMessage());
            invoiceRepository.save(invoice);

            // Notify user of failure
            emailNotificationService.sendProcessingFailed(invoice);
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Integer id, String email) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id.toString()));
        User requestor = findUserByEmail(email);
        if (requestor.getRole() != UserRole.ADMIN
                && !invoice.getUser().getId().equals(requestor.getId())) {
            throw new AccessDeniedException("You do not have access to invoice " + id);
        }
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(String vendorName, InvoiceStatus status,
                                               LocalDate fromDate, LocalDate toDate,
                                               Integer requestedUserId, Pageable pageable,
                                               String requestorEmail) {
        User requestor = findUserByEmail(requestorEmail);
        Integer effectiveUserId = (requestor.getRole() == UserRole.ADMIN)
                ? requestedUserId
                : requestor.getId();

        return invoiceRepository.searchInvoices(vendorName, status, fromDate, toDate, effectiveUserId, pageable)
                .map(this::toResponse);
    }

    // ── Admin: Update Invoice Fields ──────────────────────────────────────────

    @Transactional
    public InvoiceResponse updateInvoice(Integer id, UpdateInvoiceRequest request, String adminEmail) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id.toString()));

        if (request.getInvoiceNumber() != null
                && !request.getInvoiceNumber().equals(invoice.getInvoiceNumber())
                && invoiceRepository.existsByInvoiceNumberAndInvoiceIdNot(request.getInvoiceNumber(), id)) {
            throw new IllegalArgumentException("Invoice number already exists: " + request.getInvoiceNumber());
        }
        if (request.getInvoiceDate() != null && request.getInvoiceDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Invoice date cannot be in the future");
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (request.getInvoiceNumber() != null) invoice.setInvoiceNumber(request.getInvoiceNumber());
        if (request.getInvoiceDate()   != null) invoice.setInvoiceDate(request.getInvoiceDate());
        if (request.getAmount()        != null) invoice.setAmount(request.getAmount());
        if (request.getVendorName()    != null) invoice.setVendorName(request.getVendorName());
        if (request.getVendorAddress() != null) invoice.setVendorAddress(request.getVendorAddress());
        if (request.getComments()      != null) invoice.setComments(request.getComments());
        if (request.getStatus() != null) {
            InvoiceStatus prev = invoice.getStatus();
            invoice.setStatus(request.getStatus());
            if (prev != request.getStatus()) {
                User admin = findUserByEmail(adminEmail);
                invoice.setReviewedBy(admin);
                invoice.setReviewedAt(LocalDateTime.now());
                emailNotificationService.sendStatusChanged(invoice, request.getStatus());
            }
        }

        invoiceRepository.save(invoice);
        log.info("Invoice {} updated by admin {}", id, adminEmail);
        return toResponse(invoice);
    }

    // ── Admin: Status Change ──────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse updateInvoiceStatus(Integer id, InvoiceStatus newStatus, String comments, String reviewerEmail) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id.toString()));
        User reviewer = findUserByEmail(reviewerEmail);

        invoice.setStatus(newStatus);
        if (comments != null && !comments.isBlank()) {
            invoice.setComments(comments);
        }
        invoice.setReviewedBy(reviewer);
        invoice.setReviewedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        emailNotificationService.sendStatusChanged(invoice, newStatus);
        log.info("Invoice {} status changed to {} by {} with comments: {}", id, newStatus, reviewerEmail, comments);
        return toResponse(invoice);
    }

    // ── Retry & Delete ────────────────────────────────────────────────────────

    @Transactional
    public void retryProcessing(Integer id, String requestorEmail) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id.toString()));
        User requestor = findUserByEmail(requestorEmail);
        if (requestor.getRole() != UserRole.ADMIN
                && !invoice.getUser().getId().equals(requestor.getId())) {
            throw new AccessDeniedException("You do not have access to retry invoice " + id);
        }
        invoice.setProcessingStatus(ProcessingStatus.UPLOADED);
        invoice.setProcessingError(null);
        invoiceRepository.save(invoice);
        triggerExtractionAsync(id, invoice.getStorageBucket(), invoice.getStoragePath());
    }

    @Transactional
    public void deleteInvoice(Integer id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id.toString()));
        if (invoice.getStoragePath() != null)
            fileStorageService.deleteFile(invoice.getStoragePath());
        invoiceRepository.delete(invoice);
        log.info("Invoice deleted: {}", id);
    }

    // ── Dashboard Stats ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(String requestorEmail) {
        User requestor = findUserByEmail(requestorEmail);
        Map<String, Object> stats = new LinkedHashMap<>();

        if (requestor.getRole() == UserRole.ADMIN) {
            stats.put("totalInvoices", invoiceRepository.count());

            Map<String, Long> byStatus = new LinkedHashMap<>();
            invoiceRepository.countByStatus().forEach(r -> byStatus.put(r[0].toString(), (Long) r[1]));
            stats.put("byStatus", byStatus);

            Map<String, Long> byProcessing = new LinkedHashMap<>();
            invoiceRepository.countByProcessingStatus().forEach(r -> byProcessing.put(r[0].toString(), (Long) r[1]));
            stats.put("byProcessingStatus", byProcessing);
        } else {
            Integer userId = requestor.getId();
            stats.put("totalInvoices", invoiceRepository.countByStatusForUser(userId)
                    .stream().mapToLong(r -> (Long) r[1]).sum());

            Map<String, Long> byStatus = new LinkedHashMap<>();
            invoiceRepository.countByStatusForUser(userId)
                    .forEach(r -> byStatus.put(r[0].toString(), (Long) r[1]));
            stats.put("byStatus", byStatus);

            Map<String, Long> byProcessing = new LinkedHashMap<>();
            invoiceRepository.countByProcessingStatusForUser(userId)
                    .forEach(r -> byProcessing.put(r[0].toString(), (Long) r[1]));
            stats.put("byProcessingStatus", byProcessing);
        }
        return stats;
    }

    // ── Monthly Trend ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyTrendResponse> getMonthlyTrend(String requestorEmail) {
        User requestor = findUserByEmail(requestorEmail);
        Integer userId = requestor.getRole() == UserRole.ADMIN ? null : requestor.getId();
        LocalDate fromDate = LocalDate.now().minusMonths(11).withDayOfMonth(1);

        return invoiceRepository.getMonthlyTrend(userId, fromDate).stream()
                .map(r -> MonthlyTrendResponse.builder()
                        .month((String) r[0])
                        .total(r[3] != null ? new java.math.BigDecimal(r[3].toString()) : java.math.BigDecimal.ZERO)
                        .approved(r[4] != null ? new java.math.BigDecimal(r[4].toString()) : java.math.BigDecimal.ZERO)
                        .pending(r[5] != null ? new java.math.BigDecimal(r[5].toString()) : java.math.BigDecimal.ZERO)
                        .rejected(r[6] != null ? new java.math.BigDecimal(r[6].toString()) : java.math.BigDecimal.ZERO)
                        .build())
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File must not be empty");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        if (file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException("File size exceeds 20 MB limit");
    }

    private void applyExtractionResult(Invoice invoice, TextractCallbackRequest r) {
        String invoiceNum = r.getInvoiceNumber();

        // Business rule: unique invoice number
        if (invoiceNum != null && !invoiceNum.isBlank()
                && invoiceRepository.existsByInvoiceNumberAndInvoiceIdNot(invoiceNum, invoice.getInvoiceId())) {
            log.warn("Duplicate invoice number detected: {}", invoiceNum);
            invoice.setStatus(InvoiceStatus.DUPLICATE);
            emailNotificationService.sendDuplicateInvoiceDetected(invoice);
        }
        invoice.setInvoiceNumber(invoiceNum);

        // Business rule: invoice date must not be in the future
        LocalDate invoiceDate = parseDate(r.getInvoiceDate());
        if (invoiceDate != null && invoiceDate.isAfter(LocalDate.now())) {
            log.warn("Invoice date {} is in the future — clearing", invoiceDate);
            invoiceDate = null;
        }
        invoice.setInvoiceDate(invoiceDate);

        // Business rule: amount must be positive
        BigDecimal amount = r.getTotalAmount();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Non-positive amount {} — clearing", amount);
            amount = null;
        }
        invoice.setAmount(amount);

        if (r.getVendorName() == null || r.getVendorName().isBlank()) {
            log.warn("Vendor name missing for invoice {}", invoice.getInvoiceId());
        }
        invoice.setVendorName(r.getVendorName());
        invoice.setVendorAddress(r.getVendorAddress());

        if (r.getLineItems() != null) {
            invoice.getDetails().clear();
            r.getLineItems().forEach(li -> invoice.addDetail(InvoiceDetail.builder()
                    .itemDescription(li.getDescription())
                    .quantity(li.getQuantity())
                    .unitPrice(li.getUnitPrice())
                    .totalPrice(li.getTotalPrice())
                    .build()));
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeParseException e) { return null; }
    }

    private String resolveFileType(String ct) {
        if (ct == null) return "UNKNOWN";
        return switch (ct) {
            case "application/pdf" -> "PDF";
            case "image/jpeg"      -> "JPEG";
            case "image/png"       -> "PNG";
            case "image/tiff"      -> "TIFF";
            case "image/webp"      -> "WEBP";
            default                -> "UNKNOWN";
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    public InvoiceResponse toResponse(Invoice i) {
        String downloadUrl = null;
        try {
            if (i.getStoragePath() != null)
                downloadUrl = fileStorageService.generatePresignedUrl(i.getStoragePath());
        } catch (Exception ignored) { }

        List<InvoiceDetailResponse> details = i.getDetails() == null ? List.of() :
                i.getDetails().stream().map(d -> InvoiceDetailResponse.builder()
                        .detailId(d.getDetailId())
                        .itemDescription(d.getItemDescription())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .totalPrice(d.getTotalPrice())
                        .build()).toList();

        return InvoiceResponse.builder()
                .invoiceId(i.getInvoiceId())
                .userId(i.getUser() != null ? i.getUser().getId() : null)
                .uploadedByEmail(i.getUser() != null ? i.getUser().getEmail() : null)
                .invoiceNumber(i.getInvoiceNumber())
                .invoiceDate(i.getInvoiceDate())
                .amount(i.getAmount())
                .vendorName(i.getVendorName())
                .vendorAddress(i.getVendorAddress())
                .status(i.getStatus())
                .comments(i.getComments())
                .originalFileName(i.getOriginalFileName())
                .fileType(i.getFileType())
                .fileSizeBytes(i.getFileSizeBytes())
                .downloadUrl(downloadUrl)
                .processingStatus(i.getProcessingStatus())
                .processingError(i.getProcessingError())
                .aiConfidenceScore(i.getAiConfidenceScore())
                .aiModelUsed(i.getAiModelUsed())
                .processingDurationMs(i.getProcessingDurationMs())
                .reviewedBy(i.getReviewedBy() != null ? i.getReviewedBy().getEmail() : null)
                .reviewedAt(i.getReviewedAt())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .details(details)
                .build();
    }
}
