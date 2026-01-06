package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models;

import java.math.BigDecimal;
import java.util.List;

/**
 * Open, High, Low, Close of an entity
 */
public record OhlcSeries(
    List<BigDecimal> open,
    List<BigDecimal> high,
    List<BigDecimal> low,
    List<BigDecimal> close,
    List<Long> volumes // Yahoo often provides volume too
) {

    /**
     * Check if quote has valid close prices.
     */
    public boolean hasClosePrices() {
        return close != null && !close.isEmpty();
    }

    /**
     * Get close price at specific index (safe access).
     */
    public BigDecimal getCloseAt(int index) {
        if (close == null || index < 0 || index >= close.size()) {
            return null;
        }
        return close.get(index);
    }

    public int size() {
        return close != null ? close.size() : 0;
    }
}