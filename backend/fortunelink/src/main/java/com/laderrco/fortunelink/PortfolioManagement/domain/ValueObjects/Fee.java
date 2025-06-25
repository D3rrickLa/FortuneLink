package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.FeeType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public record Fee(FeeType feeType, Money amount) {
    public Fee {
        Objects.requireNonNull(feeType, "Fee Type cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");

        if (amount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amount cannot be negative.");
        }

        // this.amount = amount.amount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Fee that = (Fee) o;
        return Objects.equals(this.feeType, that.feeType)
                && Objects.equals(this.amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.feeType, this.amount);
    }
}