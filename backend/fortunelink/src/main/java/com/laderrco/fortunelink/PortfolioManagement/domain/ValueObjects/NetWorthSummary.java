package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

public record NetWorthSummary(Money totalAssets, Money totalLiabilities, Money netWorthValue) {
    public NetWorthSummary {
        Objects.requireNonNull(totalAssets, "Total assets must not be null.");
        Objects.requireNonNull(totalLiabilities, "Total liabilities must not be null.");
        Objects.requireNonNull(netWorthValue, "Net worth value must not be null.");

        if (!netWorthValue.equals(totalAssets.subtract(totalLiabilities))) {
            throw new IllegalArgumentException("Net worth value does not equal assets minus liabilities.");
        }
    }
}
