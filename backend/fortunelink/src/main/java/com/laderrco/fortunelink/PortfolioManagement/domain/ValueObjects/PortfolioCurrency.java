package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

// value object, make it a record
public record PortfolioCurrency(String code, String symbol) {
    public PortfolioCurrency {
        Objects.requireNonNull(code, "An exchange code must not be null.");
        Objects.requireNonNull(symbol, "A currency symbol must not be null.");
    }
}