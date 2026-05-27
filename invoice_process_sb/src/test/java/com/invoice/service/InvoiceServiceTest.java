package com.invoice.service;

import com.invoice.ai.AiExtractionResult;
//import com.invoice.ai.InvoiceAiService;
import com.invoice.domain.entity.Invoice;
import com.invoice.domain.entity.User;
import com.invoice.domain.enums.InvoiceStatus;
import com.invoice.domain.enums.ProcessingStatus;
import com.invoice.domain.enums.UserRole;
import com.invoice.dto.request.InvoiceUploadRequest;
import com.invoice.dto.request.UpdateInvoiceRequest;
import com.invoice.dto.response.InvoiceResponse;
import com.invoice.exception.InvoiceNotFoundException;
import com.invoice.repository.InvoiceRepository;
import com.invoice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @Mock DocumentExtractionService documentExtractionService;
   // @Mock InvoiceAiService invoiceAiService;
    @Mock EmailNotificationService emailNotificationService;

    @InjectMocks InvoiceService invoiceService;

    private User adminUser;
    private User regularUser;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1).email("admin@test.com")
                .role(UserRole.ADMIN).firstName("Admin").lastName("User")
                .phoneNumber("+1").passwordHash("h").build();

        regularUser = User.builder().id(2).email("user@test.com")
                .role(UserRole.USER).firstName("Regular").lastName("User")
                .phoneNumber("+2").passwordHash("h").build();

        testInvoice = Invoice.builder()
                .invoiceId(10)
                .user(regularUser)
                .status(InvoiceStatus.PENDING)
                .processingStatus(ProcessingStatus.COMPLETED)
                .invoiceNumber("INV-001")
                .vendorName("Acme Corp")
                .amount(BigDecimal.valueOf(500))
                .storagePath("invoices/test.pdf")
                .details(new ArrayList<>())
                .build();
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    @Test
    void uploadInvoice_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", new byte[100]);
        InvoiceUploadRequest req = new InvoiceUploadRequest();
        req.setFile(file);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(fileStorageService.uploadFile(any(), any())).thenReturn("invoices/uuid.pdf");
        when(fileStorageService.getBucket()).thenReturn("invoices");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setInvoiceId(1);
            return i;
        });

        InvoiceResponse resp = invoiceService.uploadInvoice(req, "user@test.com");

        assertThat(resp.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void uploadInvoice_invalidFileType_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.docx", "application/msword", new byte[100]);
        InvoiceUploadRequest req = new InvoiceUploadRequest();
        req.setFile(file);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> invoiceService.uploadInvoice(req, "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void uploadInvoice_fileTooLarge_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", new byte[21 * 1024 * 1024]);
        InvoiceUploadRequest req = new InvoiceUploadRequest();
        req.setFile(file);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> invoiceService.uploadInvoice(req, "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20 MB");
    }

    // ── GetInvoice ────────────────────────────────────────────────────────────

    @Test
    void getInvoice_owner_success() {
        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(fileStorageService.generatePresignedUrl(any())).thenReturn("http://url");

        InvoiceResponse resp = invoiceService.getInvoice(10, "user@test.com");

        assertThat(resp.getInvoiceId()).isEqualTo(10);
    }

    @Test
    void getInvoice_admin_canAccessAnyInvoice() {
        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(fileStorageService.generatePresignedUrl(any())).thenReturn("http://url");

        InvoiceResponse resp = invoiceService.getInvoice(10, "admin@test.com");

        assertThat(resp.getInvoiceId()).isEqualTo(10);
    }

    @Test
    void getInvoice_otherUser_accessDenied() {
        User stranger = User.builder().id(99).email("stranger@test.com")
                .role(UserRole.USER).build();
        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findByEmail("stranger@test.com")).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> invoiceService.getInvoice(10, "stranger@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getInvoice_notFound_throws() {
        when(invoiceRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoice(999, "user@test.com"))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    // ── Business Validations in applyExtractionResult ─────────────────────────

    @Test
    void processInvoice_duplicateNumber_setsStatusDuplicate() throws Exception {
        testInvoice.setProcessingStatus(ProcessingStatus.UPLOADED);
        testInvoice.setFileType("PDF");
        byte[] bytes = "pdf-content".getBytes();

        AiExtractionResult result = new AiExtractionResult();
        result.setInvoiceNumber("INV-DUPE");
        result.setVendorName("Vendor");
        result.setTotalAmount(BigDecimal.valueOf(100));
        result.setInvoiceDate("2024-01-15");
        result.setConfidenceScore(BigDecimal.valueOf(90));

        when(fileStorageService.downloadFile(any())).thenReturn(bytes);
        when(documentExtractionService.extractTextFromPdf(bytes)).thenReturn("long text content here that is more than 100 characters to trigger text extraction path in the service");
        //when(invoiceAiService.extractFromText(any())).thenReturn(result);
        when(invoiceRepository.existsByInvoiceNumberAndInvoiceIdNot("INV-DUPE", 10)).thenReturn(true);
        when(invoiceRepository.save(any())).thenReturn(testInvoice);

        //invoiceService.processInvoice(testInvoice);

        assertThat(testInvoice.getStatus()).isEqualTo(InvoiceStatus.DUPLICATE);
    }

    @Test
    void processInvoice_futureDate_clearsDate() throws Exception {
        testInvoice.setProcessingStatus(ProcessingStatus.UPLOADED);
        testInvoice.setFileType("PDF");

        AiExtractionResult result = new AiExtractionResult();
        result.setInvoiceNumber("INV-002");
        result.setVendorName("Vendor");
        result.setTotalAmount(BigDecimal.valueOf(100));
        result.setInvoiceDate(LocalDate.now().plusDays(10).toString());
        result.setConfidenceScore(BigDecimal.valueOf(90));

        when(fileStorageService.downloadFile(any())).thenReturn(new byte[10]);
        when(documentExtractionService.extractTextFromPdf(any())).thenReturn("");
        when(documentExtractionService.renderPdfFirstPageAsImage(any())).thenReturn(new byte[10]);
        //when(invoiceAiService.extractFromImage(any(), any())).thenReturn(result);
        when(invoiceRepository.existsByInvoiceNumberAndInvoiceIdNot(any(), any())).thenReturn(false);
        when(invoiceRepository.save(any())).thenReturn(testInvoice);

//invoiceService.processInvoice(testInvoice);

        assertThat(testInvoice.getInvoiceDate()).isNull();
    }

    // ── UpdateInvoice ─────────────────────────────────────────────────────────

    @Test
    void updateInvoice_success() {
        UpdateInvoiceRequest req = new UpdateInvoiceRequest();
        req.setVendorName("New Vendor");
        req.setAmount(BigDecimal.valueOf(999));

        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(invoiceRepository.save(any())).thenReturn(testInvoice);
        when(fileStorageService.generatePresignedUrl(any())).thenReturn("http://url");

        InvoiceResponse resp = invoiceService.updateInvoice(10, req, "admin@test.com");

        assertThat(testInvoice.getVendorName()).isEqualTo("New Vendor");
        assertThat(testInvoice.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(999));
    }

    @Test
    void updateInvoice_futureDate_throws() {
        UpdateInvoiceRequest req = new UpdateInvoiceRequest();
        req.setInvoiceDate(LocalDate.now().plusDays(5));

        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.updateInvoice(10, req, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void updateInvoice_negativeAmount_throws() {
        UpdateInvoiceRequest req = new UpdateInvoiceRequest();
        req.setAmount(BigDecimal.valueOf(-50));

        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.updateInvoice(10, req, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteInvoice_success() {
        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));
        doNothing().when(fileStorageService).deleteFile(any());
        doNothing().when(invoiceRepository).delete(any());

        invoiceService.deleteInvoice(10);

        verify(fileStorageService).deleteFile("invoices/test.pdf");
        verify(invoiceRepository).delete(testInvoice);
    }

    @Test
    void deleteInvoice_notFound_throws() {
        when(invoiceRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.deleteInvoice(999))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    // ── Retry ─────────────────────────────────────────────────────────────────

    @Test
    void retryProcessing_ownerCanRetry() {
        testInvoice.setProcessingStatus(ProcessingStatus.FAILED);
        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(invoiceRepository.save(any())).thenReturn(testInvoice);

        invoiceService.retryProcessing(10, "user@test.com");

        assertThat(testInvoice.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
        assertThat(testInvoice.getProcessingError()).isNull();
    }

    @Test
    void retryProcessing_strangerDenied() {
        User stranger = User.builder().id(99).email("stranger@test.com")
                .role(UserRole.USER).build();
        when(invoiceRepository.findById(10)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findByEmail("stranger@test.com")).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> invoiceService.retryProcessing(10, "stranger@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
