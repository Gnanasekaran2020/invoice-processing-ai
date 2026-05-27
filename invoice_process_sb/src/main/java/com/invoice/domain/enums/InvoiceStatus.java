package com.invoice.domain.enums;

public enum InvoiceStatus {
    PENDING,        // Uploaded, awaiting review
    APPROVED,       // Reviewed and approved
    REJECTED,       // Rejected by reviewer
    DUPLICATE,      // Duplicate detected
    PAID            // Marked as paid
}

