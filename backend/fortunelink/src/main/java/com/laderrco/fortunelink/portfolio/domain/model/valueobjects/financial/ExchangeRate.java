package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record ExchangeRate(Currency from, Currency to, BigDecimal rate, Instant quotedAt) {
  private static final int SCALE = Precision.FOREX.getDecimalPlaces();
  private static final RoundingMode ROUNDING = Rounding.FOREX.getMode();

  public ExchangeRate {
    notNull(from, "fromCurrency");
    notNull(to, "toCurrency");
    notNull(rate, "rate");
    notNull(quotedAt, "quoteAt");

    if (rate.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Exchange rate must be positive");
    }

    rate = rate.setScale(SCALE, ROUNDING);
  }

  public static ExchangeRate identity(Currency currency, Instant quotedAt) {
    return new ExchangeRate(currency, currency, BigDecimal.ONE, quotedAt);
  }

  public Money convert(Money money) {
    notNull(money, "money to convert");

    if (!money.currency().equals(from)) {
      throw new CurrencyMismatchException(money.currency(), from);
    }

    // Scale it down to match Money's precision before passing to the constructor
    BigDecimal rawAmount = money.amount().multiply(rate);
    BigDecimal scaledAmount = rawAmount.setScale(
        Precision.getMoneyPrecision(), Rounding.MONEY.getMode());

    return new Money(scaledAmount, to);
  }

  public ExchangeRate inverse() {
    return new ExchangeRate(to, from, BigDecimal.ONE.divide(rate, SCALE, ROUNDING), quotedAt);
  }
}