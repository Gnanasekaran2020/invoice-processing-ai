package com.invoice.dto.response;

import com.invoice.domain.enums.InvoiceStatus;
import com.invoice.domain.enums.ProcessingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    // ── invoices table fields ─────────────────────────────────────────────────
    private Integer invoiceId;
    private Integer userId;
    private String  uploadedByEmail;
    private String  invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal amount;
    private String  vendorName;
    private String  vendorAddress;
    private InvoiceStatus status;
    private String  comments;

    // ── AI / file metadata ────────────────────────────────────────────────────
    private String  originalFileName;
    private String  fileType;
    private Long    fileSizeBytes;
    private String  downloadUrl;
    private ProcessingStatus processingStatus;
    private String  processingError;
    private BigDecimal aiConfidenceScore;
    private String  aiModelUsed;
    private Long    processingDurationMs;

    // ── Review ────────────────────────────────────────────────────────────────
    private String        reviewedBy;
    private LocalDateTime reviewedAt;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── invoice_details rows ──────────────────────────────────────────────────
    private List<InvoiceDetailResponse> details;
}
