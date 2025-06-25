package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public record NetWorthSummary(Money totalAssets, Money totalLiabilities) {
    public NetWorthSummary {
        Objects.requireNonNull(totalAssets, "Total amount of assets cannot be null.");
        Objects.requireNonNull(totalAssets, "Total amount of liabilities cannot be null.");
    }

    public Money calculateNetWorth() {
        Money netWorth = totalAssets.subtract(totalLiabilities);
        return new Money(netWorth.amount(), this.totalAssets.currency());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NetWorthSummary that = (NetWorthSummary) o;
        return Objects.equals(this.totalAssets, that.totalAssets)
                && Objects.equals(this.totalLiabilities, that.totalLiabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.totalAssets, this.totalLiabilities);
    }
}
