package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo;

import lombok.Data;

// hold commanlitites between quote and chart resutls
@Data
public abstract class YahooBaseData {
    private String symbol;
    private String currency;
    private String exchange;
    
    /**
     * Get currency with fallback to USD.
     */
    public String getCurrency() {
        return currency != null ? currency : "USD";
    }
}
