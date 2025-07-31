package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

public record Money(
    BigDecimal amount, 
    Currency currency
) {
    private final static MathContext FINANCIAL_MATH_CONTEXT = MathContext.DECIMAL128; 

    public Money {
        validateParameter(amount, "constructor - amount");
        validateParameter(currency, "constructor - currency");
    }

    private void isSameCurrency(Currency other, String methodName) {
        if (!this.currency.equals(other)) {
            throw new IllegalArgumentException(String.format("Cannot %s money with different currencies. Please convert them to be the same.", methodName));
        }
    }

    private void validateParameter(Object other, String methodName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", methodName));
    }

    public Money add(Money other) {
        validateParameter(other, "add");
        isSameCurrency(other.currency(), "add");
        return new Money(this.amount.add(other.amount()), this.currency);
    }

    public Money subtract(Money other) {
        validateParameter(other, "subtract");
        isSameCurrency(other.currency(), "subtract");
        return new Money(amount.subtract(other.amount, FINANCIAL_MATH_CONTEXT), this.currency);
    }

    public Money mulitply(BigDecimal multiplier) {
        validateParameter(multiplier, "multiply");
        return new Money(this.amount.multiply(multiplier, FINANCIAL_MATH_CONTEXT), this.currency);
    }

    public Money mulitply(Double multiplier) {
        validateParameter(multiplier, "multiply");
        return mulitply(new BigDecimal(String.valueOf(multiplier)));
    }

    public Money divide(BigDecimal divisor){
        validateParameter(divisor, "divide");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Divisor cannot be zero.");
        }
        return new Money(this.amount.divide(divisor, FINANCIAL_MATH_CONTEXT).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), this.currency);
    }

    public Money divide(Double divisor) {
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
        validateParameter(other, "compareTo");
        isSameCurrency(other.currency, "compareTo");
        return this.amount.compareTo(other.amount());
    }

    public Money convertTo(Currency targetCurrency, ExchangeRate exchangeRate) {
        validateParameter(targetCurrency, "convertTo");
        validateParameter(exchangeRate, "convertTo");
        
        if (!this.currency().equals(exchangeRate.fromCurrency()) || !targetCurrency.equals(exchangeRate.toCurrency())) {
            throw new IllegalArgumentException("Exchange rate currency don't match conversion request.");
        }

        BigDecimal convertedAmount = exchangeRate.exchangeRate().multiply(this.amount());
        return new Money(convertedAmount, targetCurrency);
    }

    public static Money ZERO(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money of(BigDecimal value, Currency currency) {
        return new Money(value, currency);
    }

    public static Money of(Double value, Currency currency) {
        return new Money(new BigDecimal(String.valueOf(value)), currency);
    }
}
