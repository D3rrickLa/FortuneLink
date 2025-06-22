package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

public record NetWorthSummary(Money totalAssets, Money totalLiabilities) {
    public NetWorthSummary {
        Objects.requireNonNull(totalAssets, "Total amount of assets cannot be null.");
        Objects.requireNonNull(totalAssets, "Total amount of liabilities cannot be null.");
    }
    
    public Money calculateNetWorth() {
        Money netWorth = totalAssets.subtract(totalLiabilities);
        return new Money(netWorth.amount(), this.totalAssets.currency());
    }
}
