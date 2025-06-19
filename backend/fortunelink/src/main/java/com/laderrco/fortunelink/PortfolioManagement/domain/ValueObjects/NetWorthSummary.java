package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

public record NetWorthSummary(Money totalAsset, Money totalLiabilities, Money netWorthValue) {
    public NetWorthSummary {
        Objects.requireNonNull(totalAsset);
        Objects.requireNonNull(totalLiabilities);
    }
}
