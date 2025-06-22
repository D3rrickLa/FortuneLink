package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public record Money(BigDecimal amount, PortfolioCurrency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(currency, "Currency cannot be null.");

        amount = amount.setScale(currency.getDefaultScale(), RoundingMode.HALF_UP);
    }

    // What can we do with money? we can add, subtract, multiple, and divide
    public Money add(Money other) {
        Objects.requireNonNull(other, "Cannot pass null to the 'add' function");
        if (!this.currency.equals(other.currency())) {
            throw new IllegalArgumentException("Cannot add Money with different currencies. Convert first: "  + this.currency + " vs " + other.currency());
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Amount of money to subtract cannot be null.");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract Money with currencies. Convert first: " + this.currency + " vs " + other.currency());
        }
        return new Money(amount.subtract(other.amount), this.currency);
    }

    public Money multiply(Long multiplier) {
        return mulitply(new BigDecimal(multiplier));
    }
    
    public Money mulitply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), this.currency);
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("CAnnot divide by zero.");
        }
        return new Money(amount.divide(divisor, this.currency.getDefaultScale(), RoundingMode.HALF_UP), this.currency);
    }

    public int compareTo(Money other) {
        if (!this.currency.equals(other.currency())) {
            throw new IllegalArgumentException("Cannot compare Money with different currencies. Convert first: " + currency() + " vs " + other.currency());
        }
        return this.amount.compareTo(other.amount());
    }

}
