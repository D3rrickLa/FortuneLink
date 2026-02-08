package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

public record Money(BigDecimal amount, Currency currency) implements ClassValidation, Comparable<Money> {
    private static final int MONEY_PRECISION = Precision.getMoneyPrecision();
    private static final RoundingMode M_ROUNDING_MODE = Rounding.MONEY.getMode();

    public Money {
        ClassValidation.validateParameter(amount, "Amount must not be null");
        ClassValidation.validateParameter(currency, "Currency must not be null");

        if (amount.scale() > MONEY_PRECISION) {
            throw new IllegalArgumentException("Amount exceeds allowed precision");
        }

        amount = normalize(amount);
    }

    public static Money of(BigDecimal value, String currency) {
        return new Money(new BigDecimal(String.valueOf(value)), Currency.of(currency));
    }

    public static Money of(double value, String currency) {
        return new Money(new BigDecimal(String.valueOf(value)), Currency.of(currency));
    }

    public static Money ZERO(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money ZERO(String currency) {
        return ZERO(Currency.of(currency));
    }

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
        return new Money(applyScale(amount.multiply(multiplier)), currency);
    }

    public Money multiply(Quantity multiplier) {
        ClassValidation.validateParameter(multiplier, "multiply");
        return multiply(multiplier.amount());
    }

    public Money divide(BigDecimal divisor) {
        ClassValidation.validateParameter(divisor, "divide");
        if (BigDecimal.ZERO.compareTo(divisor) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, MONEY_PRECISION, M_ROUNDING_MODE), this.currency);
    }

    public Money divide(Quantity divisor) {
        return divide(divisor.amount());
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

    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public boolean exceeds(Money other) {
        isSameCurrency(other.currency(), "exceeds");
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isAtLeast(Money other) {
        isSameCurrency(other.currency(), "isAtLeast");
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        isSameCurrency(other.currency(), "isLessThan");
        return this.amount.compareTo(other.amount) < 0;
    }

    @Override
    public int compareTo(Money other) {
        isSameCurrency(other.currency(), "compareTo");
        BigDecimal normalizedValue = normalize(other.amount());
        return normalize(this.amount).compareTo(normalizedValue);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_PRECISION, M_ROUNDING_MODE);
    }

    private static BigDecimal applyScale(BigDecimal value) {
        return value.setScale(MONEY_PRECISION, M_ROUNDING_MODE);
    }

    private void validateMoney(Money other, String operation) {
        ClassValidation.validateParameter(other, operation);
        isSameCurrency(other.currency(), operation);
    }

    private void isSameCurrency(Currency otherCurrency, String operation) {
        if (!this.currency.equals(otherCurrency)) {
            throw new CurrencyMismatchException(this.currency, otherCurrency);
        }
    }

}
