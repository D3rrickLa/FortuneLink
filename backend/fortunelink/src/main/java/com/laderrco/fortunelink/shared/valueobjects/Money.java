package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;

public record Money(BigDecimal amount, ValidatedCurrency currency) implements ClassValidation {
    private static final MathContext FINANCIAL_MATH_CONTEXT = MathContext.DECIMAL128;

    public Money {
        amount = ClassValidation.validateParameter(amount);
        currency = ClassValidation.validateParameter(currency);

        amount = amount.setScale(Precision.getMoneyPrecision(), Rounding.MONEY.getMode());
    }

        public static Money of(BigDecimal value, ValidatedCurrency currency) {
        return new Money(value, currency);
    }

    public static Money of(double value, String currency) {
        return of(new BigDecimal(String.valueOf(value)), ValidatedCurrency.of(currency));
    }

    public static Money ZERO(ValidatedCurrency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money ZERO(String currency) {
        return ZERO(ValidatedCurrency.of(currency));
    }

    // OPERATION FUNTIONS //
    public Money add(Money other) {
        validateMoney(other, "add");
        return new Money(this.amount.add(other.amount()), this.currency);
    }

    public Money subtract(Money other) {
        validateMoney(other, "subtract");
        return new Money(this.amount.subtract(other.amount()), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        ClassValidation.validateParameter(multiplier, "multiply");
        return new Money(this.amount.multiply(multiplier, FINANCIAL_MATH_CONTEXT), this.currency);
    }

    public Money divide(BigDecimal divisor) {
        ClassValidation.validateParameter(divisor, "divide");
        if (divisor.equals(BigDecimal.ZERO)) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, Precision.getMoneyPrecision(), Rounding.MONEY.getMode()), this.currency);
    }


    // LOGIC FUNCTIONS //
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        isCurrencyTheSame(other.currency(), "isGreaterThan");
        return this.amount.compareTo(other.amount) > 0;
    } 

    public boolean isLessThan(Money other) {
        isCurrencyTheSame(other.currency(), "isLessThan");
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public int compareTo(Money other) {
        isCurrencyTheSame(other.currency(), "compareTo");
        return this.amount.compareTo(other.amount());
    }

    public Money min(Money other) {
        validateMoney(other, "min");
        BigDecimal minAmount = this.amount.min(other.amount());
        return new Money(minAmount, this.currency); 
    }

    public Money max(Money other) {
        validateMoney(other, "max");
        BigDecimal maxAmount = this.amount.max(other.amount());
        return new Money(maxAmount, this.currency); 
    }

    public Money convert(ExchangeRate rate) {
        ClassValidation.validateParameter(rate, "convert");
        return rate.convert(this);
    }

    public Percentage toPercentage() {
        return new Percentage(this.amount);
    }
    

    private void validateMoney(Money other, String operation) {
        Objects.requireNonNull(other, String.format("Cannot %s null money", operation));
        isCurrencyTheSame(other.currency(), operation);

    }

    private void isCurrencyTheSame(ValidatedCurrency otherCurrency, String operation) {
        if (!this.currency.equals(otherCurrency)) {
            throw new CurrencyMismatchException(String.format("Cannot %s different currencies", operation));
        }
    }
}