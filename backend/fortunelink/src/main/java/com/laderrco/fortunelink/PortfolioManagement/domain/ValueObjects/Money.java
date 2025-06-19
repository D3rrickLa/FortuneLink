package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, PortfolioCurrency currencyCode) {
    // what can we do with money?
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null.");

        if (amount.scale() < 4) {
            amount = amount.setScale(4, RoundingMode.HALF_UP);
        }

        if (currencyCode.code().length() != 3) {
            // ISO 4217 standard enforcement
            throw new IllegalArgumentException("Currency code must be 3 characters long.");
        }

        currencyCode.code().toUpperCase();
    }

    public Money add(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException("Cannot add money with different currencies.");
        }
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies.");
        }
        return new Money(this.amount.subtract(other.amount), this.currencyCode);
    }

    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier cannot be null.");
        return new Money(this.amount.multiply(multiplier), this.currencyCode);
    }

    public boolean isPostivie() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public Money negate() {
        amount.negate();
        return this;
    }
}