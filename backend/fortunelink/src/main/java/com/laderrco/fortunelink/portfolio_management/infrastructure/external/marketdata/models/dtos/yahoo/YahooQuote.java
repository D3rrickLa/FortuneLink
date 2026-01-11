package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo;

import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class YahooQuote extends YahooBaseData {
    private String shortName;
    private String longName;
    private String quoteType;
    private BigDecimal regularMarketPrice;
    private BigDecimal bid;
    private BigDecimal ask;
    private Long regularMarketTime;

    /**
     * Get the most appropriate price.
     * Prefers regularMarketPrice, falls back to bid, then ZERO.
     */
    public BigDecimal getPrice() {
        if (regularMarketPrice != null && regularMarketPrice.compareTo(BigDecimal.ZERO) > 0) {
            return regularMarketPrice;
        }
        if (bid != null && bid.compareTo(BigDecimal.ZERO) > 0) {
            return bid;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get timestamp or current time if null.
     */
    public long getTimestampOrNow() {
        return regularMarketTime != null ? regularMarketTime : System.currentTimeMillis() / 1000;
    }
}