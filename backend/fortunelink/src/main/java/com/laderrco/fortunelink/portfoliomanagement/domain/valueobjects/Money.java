package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.Rounding;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;

public record Money(
    BigDecimal amount, 
    Currency currency
) {
    private static final MathContext FINANCIAL_MATH_CONTEXT = MathContext.DECIMAL128;

    public Money {
        validateParameter(amount, "Amount");
        validateParameter(currency, "Currency");

        amount = amount.setScale(DecimalPrecision.MONEY.getDecimalPlaces(), Rounding.MONEY.getMode());
    }

    public static Money of(BigDecimal value, Currency currency) {
        return new Money(value, currency);
    }

    public static Money of(double value, Currency currency) {
        return new Money(BigDecimal.valueOf(value), currency);
    }

    public static Money of(BigDecimal value, String currency) {
        return new Money(value, Currency.getInstance(currency));
    }
    
    public static Money of(double value, String currency) {
        return new Money(BigDecimal.valueOf(value), Currency.getInstance(currency));
    }
    
    public static Money ZERO(Currency currency) { 
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money ZERO(String currency) {
        return new Money(BigDecimal.ZERO, Currency.getInstance(currency));
    }

    public Money add(Money other) {
        validate(other, "Money", "add");
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) { 
        validate(other, "Money", "subtract");
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    public Money multiply(BigDecimal multiplier) { 
        validateParameter(multiplier, "Multiplier");
        return new Money(this.amount.multiply(multiplier, FINANCIAL_MATH_CONTEXT), this.currency); 
    }
    public Money multiply(double multiplier) { 
        validateParameter(multiplier, "Multiplier");
        return new Money(this.amount.multiply(new BigDecimal(String.valueOf(multiplier)), FINANCIAL_MATH_CONTEXT), this.currency); 
    }
    public Money divided(BigDecimal divisor) {
        validateParameter(divisor, "Divisor");
        return new Money(this.amount.divide(divisor, FINANCIAL_MATH_CONTEXT), this.currency); 
    }
    public Money divided(double divisor) {
        validateParameter(divisor, "Divisor");
        return new Money(this.amount.divide(new BigDecimal(String.valueOf(divisor)), FINANCIAL_MATH_CONTEXT), this.currency); 
    }

    public Money convertTo(Currency targetCurrency, CurrencyConversion exchangeRate) {
        validateParameter(targetCurrency, "Target currency");   
        validateParameter(exchangeRate, "Exchange rate");
        
        if (!this.currency().equals(exchangeRate.fromCurrency()) || !targetCurrency.equals(exchangeRate.toCurrency())) {
            throw new CurrencyMismatchException("Exchange rate 'to currency' don't match target currency.");
        }

        BigDecimal convertedAmount = exchangeRate.exchangeRate().multiply(this.amount(), FINANCIAL_MATH_CONTEXT);
        return new Money(convertedAmount, targetCurrency);
    }
    
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        validateParameter(other, "Money");
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        validateParameter(other, "Money");
        return this.amount.compareTo(other.amount) < 0;
    }

    public int compareTo(Money other) {
        validate(other, "Money", "compareTo");
        return this.amount.compareTo(other.amount);
    }

    public Money min(Money other) {
        validate(other, "Money", "min");
        BigDecimal minAmount = this.amount.min(other.amount());
        return new Money(minAmount, this.currency); 
    }
    public Money max(Money other) {
        validate(other, "Money", "max");
        BigDecimal maxAmount = this.amount.max(other.amount());
        return new Money(maxAmount, this.currency); 
    }

    private void validate(Money other, String parameterName, String methodName) {
        validateParameter(other, parameterName);
        isSameCurrency(other.currency(), methodName);
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

    private void isSameCurrency(Currency other, String methodName) {
        if (this.currency.equals(other) == false) {
            throw new CurrencyMismatchException(String.format("Cannot %s money with different currencies. Please conver them to be the same.", methodName));
        }
    }

}