package com.invoice.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload posted by the Textract Lambda back to
 * POST /api/invoices/{id}/extraction-callback.
 *
 * The Lambda maps AWS Textract AnalyzeExpense SummaryFields and
 * LineItemGroups to this structure before calling the callback URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextractCallbackRequest {

    private String     invoiceNumber;
    private String     vendorName;
    private String     vendorAddress;
    private String     invoiceDate;          // YYYY-MM-DD
    private String     dueDate;              // YYYY-MM-DD
    private BigDecimal totalAmount;
    private String     currency;
    /** 0–100 confidence derived from Textract block confidence scores. */
    private Integer    confidenceScore;
    /** Wall-clock ms the Lambda spent on Textract + parsing. */
    private Long       processingDurationMs;
    private List<LineItem> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineItem {
        private String     description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
