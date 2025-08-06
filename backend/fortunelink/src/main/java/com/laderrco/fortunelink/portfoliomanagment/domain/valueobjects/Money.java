package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

// need to deal with the scaling
public record Money(
    BigDecimal amount, 
    Currency currency
) {
    private final static MathContext FINANCIAL_MATH_CONTEXT = MathContext.DECIMAL128; 

    public Money {
        validateParameter(amount, "Amount");
        validateParameter(currency, "Currency");

        amount = amount.setScale(DecimalPrecision.MONEY.getDecimalPlaces(), RoundingMode.HALF_EVEN); // TODO remove or keep? find out
    }
    
    private void isSameCurrency(Currency other, String methodName) {
        if (!this.currency.equals(other)) {
            throw new IllegalArgumentException(String.format("Cannot %s money with different currencies. Please convert them to be the same.", methodName));
        }
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

    public Money add(Money other) {
        validateParameter(other, "Money");
        isSameCurrency(other.currency(), "add");
        return new Money(this.amount.add(other.amount()), this.currency);
    }

    public Money subtract(Money other) {
        validateParameter(other, "Money");
        isSameCurrency(other.currency(), "subtract");
        return new Money(amount.subtract(other.amount, FINANCIAL_MATH_CONTEXT), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        validateParameter(multiplier, "Multiplier");
        return new Money(this.amount.multiply(multiplier, FINANCIAL_MATH_CONTEXT), this.currency);
    }

    public Money multiply(Double multiplier) {
        validateParameter(multiplier, "Multiplier");
        return multiply(new BigDecimal(String.valueOf(multiplier)));
    }

    public Money divide(BigDecimal divisor){
        validateParameter(divisor, "Divisor");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Divisor cannot be zero.");
        }
        return new Money(this.amount.divide(divisor, FINANCIAL_MATH_CONTEXT), this.currency);
    }

    public Money divide(Double divisor) {
        validateParameter(divisor, "Divisor");
        return divide(new BigDecimal(String.valueOf(divisor)));
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public int compareTo(Money other) {
        validateParameter(other, "Money");
        isSameCurrency(other.currency, "compareTo");
        return this.amount.compareTo(other.amount());
    }

    public Money convertTo(Currency targetCurrency, ExchangeRate exchangeRate) {
        validateParameter(targetCurrency, "Target currency");
        validateParameter(exchangeRate, "Exchange rate");
        
        if (!this.currency().equals(exchangeRate.fromCurrency()) || !targetCurrency.equals(exchangeRate.toCurrency())) {
            throw new IllegalArgumentException("Exchange rate currency don't match conversion request.");
        }

        BigDecimal convertedAmount = exchangeRate.exchangeRate().multiply(this.amount(), FINANCIAL_MATH_CONTEXT);
        System.out.println(convertedAmount);
        return new Money(convertedAmount, targetCurrency);
    }

    public Money normalizedForDisplay() {
        return new Money(this.amount.setScale(DecimalPrecision.MONEY.getDecimalPlaces()), this.currency);
    }

    public static Money ZERO(Currency currency) {
        Objects.requireNonNull(currency, "Currency cannot be null.");
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money of(BigDecimal value, Currency currency) {
        Objects.requireNonNull(value, "Value cannot be null.");
        Objects.requireNonNull(currency, "Currency cannot be null.");
        return new Money(value, currency);
    }

    public static Money of(double value, Currency currency) {
        Objects.requireNonNull(value, "Value cannot be null.");
        Objects.requireNonNull(currency, "Currency cannot be null.");
        return new Money(new BigDecimal(String.valueOf(value)), currency);
    }

    public static Money of(long value, Currency currency) {
        Objects.requireNonNull(value, "Value cannot be null.");
        Objects.requireNonNull(currency, "Currency cannot be null.");
        return new Money(new BigDecimal(String.valueOf(value)), currency);
    }
}
