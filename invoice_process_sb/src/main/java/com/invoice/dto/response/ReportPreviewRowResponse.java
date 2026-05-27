package com.invoice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReportPreviewRowResponse {
    private String invoiceNumber;
    private String vendor;
    private String date;
    private BigDecimal amount;
    private String status;
}

