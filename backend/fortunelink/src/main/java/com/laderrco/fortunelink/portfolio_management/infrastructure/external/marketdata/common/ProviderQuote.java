package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Internal representation of a price quote from any provider.
 * Part of the Anti-Corruption Layer - isolates domain from provider specifics.
 */
public record ProviderQuote(String symbol, BigDecimal price, String currency, LocalDateTime timestamp, String source  /*"YAHOO", "ALPHAVANTAGE", etc. */) {
    public ProviderQuote {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }
}