package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.Precision;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.Rounding;

public record Money(BigDecimal amount, Currency currency) {
    private static final MathContext FINANCIAL_MATH_CONTEXT = MathContext.DECIMAL128;
    
    public Money {
        isParemeterNull(amount, "amount");
        isParemeterNull(currency, "currency");
        amount = amount.setScale(Precision.getMoneyDecimalPlaces(), Rounding.MONEY.getMode());
    }

    public static Money of(BigDecimal value, Currency currency) {
        return new Money(value, currency);
    }

    public static Money of(double value, String currency) {
        return of(new BigDecimal(String.valueOf(value)), Currency.getInstance(currency));
    }

    public static Money ZERO(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money ZERO(String currency) {
        return ZERO(Currency.getInstance(currency));
    }


    /**
     * REQUIREMENTS:
     *  add two amounts with same currency [X]
     *  reject two currencies  
     *  handle zero amounts -> does nothing [X]
     *  handle negative amounts  -> allows [X]
     *  validate null [X]
     * 
     * 
     * @param other
     * @return new Money object with summed amount from this and other.amount
     */
    public Money add(Money other) {
        validateMoney(other, "add");
        return new Money(this.amount.add(other.amount()), this.currency);
    }

    public Money subtract(Money other) {
        validateMoney(other, "subtract");
        return new Money(this.amount.subtract(other.amount()), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        isParemeterNull(multiplier, "multiply");
        return new Money(this.amount.multiply(multiplier, FINANCIAL_MATH_CONTEXT), this.currency);
    }

    public Money divide(BigDecimal divisor) {
        isParemeterNull(divisor, "divide");
        if (divisor.equals(BigDecimal.ZERO)) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, Precision.getMoneyDecimalPlaces(), Rounding.MONEY.getMode()), this.currency);
    }

    private void isParemeterNull(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null", parameterName));
    }

    private void validateMoney(Money other, String operation) {
        Objects.requireNonNull(other, String.format("Cannot %s null money", operation));
        isCurrencyTheSame(other.currency(), operation);

    }

    private void isCurrencyTheSame(Currency otherCurrency, String operation) {
        if (!this.currency.equals(otherCurrency)) {
            throw new CurrencyMismatchException(String.format("Cannot %s different currencies", operation));
        }
    }
}