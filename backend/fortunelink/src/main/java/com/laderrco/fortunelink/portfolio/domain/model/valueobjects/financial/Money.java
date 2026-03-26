package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {
  private static final int MONEY_PRECISION = Precision.getMoneyPrecision();
  private static final RoundingMode M_ROUNDING_MODE = Rounding.MONEY.getMode();

  public Money {
    notNull(amount, "amount");
    notNull(currency, "currency");
    amount = normalize(amount); // clamp - don't throw on extra scale
  }

  public static Money of(BigDecimal value, String currency) {
    return new Money(new BigDecimal(String.valueOf(value)), Currency.of(currency));
  }

  public static Money of(double value, String currency) {
    return new Money(new BigDecimal(String.valueOf(value)), Currency.of(currency));
  }

  public static Money of(String value, Currency currency) {
    return new Money(new BigDecimal(value), currency);
  }

  public static Money zero(Currency currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  public static Money zero(String currency) {
    return zero(Currency.of(currency));
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(MONEY_PRECISION, M_ROUNDING_MODE);
  }

  public static Money of(int i, Currency usd) {
    return new Money(BigDecimal.valueOf(i), usd);
  }

  public Money add(Money other) {
    validateMoney(other, "add");
    return new Money(normalize(this.amount.add(other.amount())), this.currency);
  }

  public Money subtract(Money other) {
    validateMoney(other, "subtract");
    return new Money(normalize(this.amount.subtract(other.amount())), this.currency);
  }

  public Money multiply(BigDecimal multiplier) {
    notNull(multiplier, "multiplier");
    return new Money(normalize(this.amount.multiply(multiplier)), this.currency);
  }

  public Money multiply(Quantity multiplier) {
    notNull(multiplier, "multiplier");
    return multiply(multiplier.amount());
  }

  public Money divide(BigDecimal divisor) {
    notNull(divisor, "divisor");
    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
      throw new ArithmeticException("Cannot divide by zero");
    }
    return new Money(this.amount.divide(divisor, MONEY_PRECISION, M_ROUNDING_MODE), this.currency);
  }

  public Money divide(Quantity divisor) {
    return divide(divisor.amount());
  }

  public Money abs() {
    return new Money(this.amount.abs(), this.currency);
  }

  public Money negate() {
    return new Money(this.amount.negate(), this.currency);
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

  public boolean exceeds(Money other) {
    requireSameCurrency(other.currency(), "exceeds");
    return this.amount.compareTo(other.amount) > 0;
  }

  public boolean isAtLeast(Money other) {
    requireSameCurrency(other.currency(), "isAtLeast");
    return this.amount.compareTo(other.amount) >= 0;
  }

  public boolean isLessThan(Money other) {
    requireSameCurrency(other.currency(), "isLessThan");
    return this.amount.compareTo(other.amount) < 0;
  }

  @Override
  public int compareTo(Money other) {
    requireSameCurrency(other.currency(), "compareTo");
    return this.amount.compareTo(other.amount());
  }

  private void validateMoney(Money other, String operation) {
    notNull(other, operation);
    requireSameCurrency(other.currency(), operation);
  }

  private void requireSameCurrency(Currency otherCurrency, String operation) {
    if (!this.currency.equals(otherCurrency)) {
      throw new CurrencyMismatchException(this.currency, otherCurrency, operation);
    }
  }
}
