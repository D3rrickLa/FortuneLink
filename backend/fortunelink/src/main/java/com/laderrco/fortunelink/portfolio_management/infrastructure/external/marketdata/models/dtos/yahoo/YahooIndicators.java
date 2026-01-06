package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.OhlcSeries;

/**
 * Logically separate because Yahoo might add 'technical indicators'
 * like RSI or Moving Averages here in different API versions.
 */
public record YahooIndicators(List<OhlcSeries> quote, @JsonProperty("adjclose") List<AdjClose> adjClose) {
    public record AdjClose(List<BigDecimal> adjclose) {}

    public OhlcSeries getPrimarySeries() {
        return (quote != null && !quote.isEmpty()) ? quote.get(0) : null;
    }

    public boolean hasQuoteData() {
        return quote != null && !quote.isEmpty();
    }

    /**
     * Get the first (and usually only) quote data.
     */
    public OhlcSeries getFirstQuote() {
        return hasQuoteData() ? quote.get(0) : null;
    }
}