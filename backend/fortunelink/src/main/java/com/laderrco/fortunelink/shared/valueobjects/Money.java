package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(currency, "Currency cannot be null.");

        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    public Money (double amount, Currency currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    private void areSameCurrency(Currency other, String methodName) {
        if (!this.currency.equals(other)) {
            throw new IllegalArgumentException(String.format(
                "Cannot %s money with different currencies. Please convert them to be the same.",
                methodName
            ));
        }

    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Cannot pass null to the 'add' method.");
        areSameCurrency(other.currency(), "add");
        return new Money(this.amount.add(other.amount()), this.currency);
    }
    
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Cannot pass null to the 'subtract' method.");
        areSameCurrency(other.currency, "subtract");
        return  new Money(amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier cannot be null.");
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    public Money multiply(Long multiplier) {
        return multiply(new BigDecimal(String.valueOf(multiplier)));
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null.");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Divisor cannot be zero.");
        }
        return new Money(this.amount.divide(divisor, this.currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN), this.currency);
    }
    
    public Money divide(Long divisor) {
        return divide(new BigDecimal(String.valueOf(divisor)));
    }
    
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    public boolean isZero() {
        if (amount.compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }
        return true;
    }

    public boolean isPositive() {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        return true;
        
    }
    
    public boolean isNegative() {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }
        return true;        
    }

    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Cannot pass null to the 'compareTo' method.");
        areSameCurrency(other.currency(), "compareTo");
        return this.amount.compareTo(other.amount());
    }

    public Money convertTo(Currency targetCurrency, ExchangeRate rate) {
        Objects.requireNonNull(targetCurrency, "Cannot pass target currency as null to the 'convertTo' method.");
        Objects.requireNonNull(rate, "Cannot pass exchange rate as null to the 'convertTo' method.");

        if (!this.currency().equals(rate.fromCurrency()) || !targetCurrency.equals(rate.toCurrency())) {
            throw new IllegalArgumentException("Exchange rate currencies don't match conversion request.");
        }

        BigDecimal convertedAmount = rate.rate().multiply(this.amount());
        return new Money(convertedAmount, targetCurrency);
    }


    public static Money ZERO(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
}
