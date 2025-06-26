package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

// we are returning new because of immutability
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
        return multiply(new BigDecimal(multiplier));
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), this.currency);
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("CAnnot divide by zero.");
        }
        return new Money(amount.divide(divisor, this.currency.getDefaultScale(), RoundingMode.HALF_UP), this.currency);
    }

    public static Money ZERO(PortfolioCurrency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public int compareTo(Money other) {
        if (!this.currency.equals(other.currency())) {
            throw new IllegalArgumentException("Cannot compare Money with different currencies. Convert first: " + currency() + " vs " + other.currency());
        }
        return this.amount.compareTo(other.amount());
    }

    public Money setScale(int newScale, RoundingMode roundingMode) {
        return new Money(this.amount.setScale(newScale, roundingMode), this.currency);
    }

    
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Money that = (Money) o;
        return Objects.equals(this.amount, that.amount)
                && Objects.equals(this.currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.currency, this.amount);
    }

}
