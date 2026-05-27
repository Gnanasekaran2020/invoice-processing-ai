package com.invoice.controller;

import com.invoice.domain.enums.InvoiceStatus;
import com.invoice.dto.request.InvoiceUploadRequest;
import com.invoice.dto.request.TextractCallbackRequest;
import com.invoice.dto.request.UpdateInvoiceRequest;
import com.invoice.dto.response.ApiResponse;
import com.invoice.dto.response.InvoiceResponse;
import com.invoice.dto.response.MonthlyTrendResponse;
import com.invoice.security.JwtTokenProvider;
import com.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice upload, processing and management")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final JwtTokenProvider jwtTokenProvider;
    /** Upload invoice — any authenticated user */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an invoice (PDF/Image) — triggers async AI extraction")
    public ResponseEntity<ApiResponse<InvoiceResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "notes", required = false) String notes,
            @AuthenticationPrincipal UserDetails userDetails) {

        InvoiceUploadRequest req = new InvoiceUploadRequest();
        req.setFile(file);
        req.setNotes(notes);
        InvoiceResponse resp = invoiceService.uploadInvoice(req, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice uploaded. AI processing started.", resp));
    }

    /** Get invoice by ID — owner or Admin */
    @GetMapping("/{id}")
    @Operation(summary = "Get invoice details by ID (owner or Admin)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.getInvoice(id, userDetails.getUsername())));
    }

    /** List invoices — Admin sees all, User sees own */
    @GetMapping
    @Operation(summary = "List invoices with search/filter (Admin: all | User: own)")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> list(
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.listInvoices(vendorName, status, fromDate, toDate,
                        userId, pageable, userDetails.getUsername())));
    }

    /** Update invoice fields — ADMIN only (secured in SecurityConfig) */
    @PutMapping("/{id}")
    @Operation(summary = "[ADMIN] Update invoice fields")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateInvoiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Invoice updated",
                invoiceService.updateInvoice(id, request, userDetails.getUsername())));
    }

    /** Change status — ADMIN only (secured in SecurityConfig) */
    @PutMapping("/{id}/status")
    @Operation(summary = "[ADMIN] Change invoice status (APPROVED / REJECTED / PAID) with optional comments")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam InvoiceStatus newStatus,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success("Status updated to " + newStatus,
                invoiceService.updateInvoiceStatus(id, newStatus, comments, userDetails.getUsername())));
    }

    /** Retry AI processing — owner or Admin */
    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry AI processing for a failed invoice")
    public ResponseEntity<ApiResponse<Void>> retry(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        invoiceService.retryProcessing(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Processing re-triggered", null));
    }

    /** Delete invoice — ADMIN only (secured in SecurityConfig) */
    @DeleteMapping("/{id}")
    @Operation(summary = "[ADMIN] Delete an invoice and its stored file")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice deleted", null));
    }

    /** Dashboard stats — role-filtered */
    @GetMapping("/stats/dashboard")
    @Operation(summary = "Dashboard statistics (role-filtered)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.getDashboardStats(userDetails.getUsername())));
    }

    /** Monthly trend — last 12 months, role-filtered */
    @GetMapping("/stats/monthly-trend")
    @Operation(summary = "Monthly invoice trend — last 12 months (role-filtered)")
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> monthlyTrend(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.getMonthlyTrend(userDetails.getUsername())));
    }

    /**
     * Internal callback posted by the Textract Lambda after extraction completes.
     * Secured by a real signed JWT passed as Authorization: Bearer <token>.
     * This endpoint is permitAll in SecurityConfig — JWT is validated manually in the service.
     */
    @PostMapping("/{id}/extraction-callback")
    @Operation(summary = "[INTERNAL] Textract Lambda posts extraction results here — not for direct use")
    public ResponseEntity<ApiResponse<Void>> extractionCallback(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody TextractCallbackRequest payload) {
        invoiceService.handleExtractionCallback(id, authorizationHeader, payload);
        return ResponseEntity.ok(ApiResponse.success("Extraction result applied", null));
    }
    /** Generate a signed JWT for the internal Lambda service identity — no auth required */
    @GetMapping("/lambda-token-internal")
    @Operation(summary = "Generate a signed JWT for lambda-service@internal — used by Textract Lambda to call back")
    public ResponseEntity<String> getLambdaInternalToken() {
        // Always issues a token for the fixed internal identity.
        // Caller identity is verified in handleExtractionCallback().
        String token = jwtTokenProvider.generateToken("lambda-service@internal");
        return ResponseEntity.ok(token);
    }
}
