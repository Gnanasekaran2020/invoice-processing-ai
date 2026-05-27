package com.invoice.dto.request;

import com.invoice.domain.enums.InvoiceStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateInvoiceRequest {
    private String invoiceNumber;
    private LocalDate invoiceDate;

    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String vendorName;
    private String vendorAddress;
    private InvoiceStatus status;
    private String comments;
}
