package com.invoice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlyTrendResponse {
    private String month;
    private BigDecimal total;
    private BigDecimal approved;
    private BigDecimal pending;
    private BigDecimal rejected;
}

