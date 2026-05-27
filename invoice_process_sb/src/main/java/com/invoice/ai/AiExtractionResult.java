package com.invoice.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Structured result returned from the AI model after invoice analysis.
 * This maps directly from the JSON response produced by GPT-4o Vision.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiExtractionResult {

    private String invoiceNumber;
    private String vendorName;
    private String vendorAddress;
    private String vendorEmail;
    private String vendorTaxId;
    private String customerName;
    private String customerAddress;
    private String invoiceDate;       // ISO-8601 string, parsed later
    private String dueDate;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal taxRate;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String paymentTerms;
    private String bankAccount;
    private String iban;
    private String swiftBic;
    private String poNumber;
    private String notes;

    private List<LineItemResult> lineItems;

    /** 0–100 confidence score produced by the model itself */
    private BigDecimal confidenceScore;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineItemResult {
        private Integer lineNumber;
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal totalPrice;
        private String productCode;
    }
}

