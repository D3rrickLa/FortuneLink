package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConvertRequest {
    private BigDecimal amount;
    private String currency;
    private String targetCurrency;
}