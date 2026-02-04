package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;

public record RealizedGain(
        TransactionId transactionId,
        AssetSymbol assetSymbol,
        Money costBasis,
        Money proceeds,
        Money gainLoss,
        List<TaxLot> lotsConsumed,
        Instant soldAt) {
    public RealizedGain {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (assetSymbol == null) {
            throw new IllegalArgumentException("Asset symbol cannot be null");
        }
        if (costBasis == null) {
            throw new IllegalArgumentException("Cost basis cannot be null");
        }
        if (proceeds == null) {
            throw new IllegalArgumentException("Proceeds cannot be null");
        }
        if (gainLoss == null) {
            throw new IllegalArgumentException("Gain/loss cannot be null");
        }
        if (soldAt == null) {
            throw new IllegalArgumentException("Sold at cannot be null");
        }

        lotsConsumed = lotsConsumed == null ? List.of() : List.copyOf(lotsConsumed);

        // Validate currencies match
        if (!costBasis.currency().equals(proceeds.currency())) {
            throw new IllegalArgumentException(
                    "Cost basis and proceeds must have same currency");
        }
        if (!costBasis.currency().equals(gainLoss.currency())) {
            throw new IllegalArgumentException(
                    "Cost basis and gain/loss must have same currency");
        }
    }

    /**
     * Check if this is a long-term capital gain (held > 365 days).
     * Uses the earliest lot's acquisition date.
     */
    public boolean isLongTerm() {
        if (lotsConsumed.isEmpty()) {
            return false;
        }

        // For FIFO, the first lot consumed is the earliest acquired
        TaxLot earliestLot = lotsConsumed.get(0);
        return earliestLot.isLongTerm(soldAt);
    }

    /**
     * Get the holding period in days (based on earliest lot).
     */
    public long getHoldingPeriodDays() {
        if (lotsConsumed.isEmpty()) {
            return 0;
        }

        TaxLot earliestLot = lotsConsumed.get(0);
        return earliestLot.getHoldingPeriodDays(soldAt);
    }

}