package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Represents the conversion ratio between two currencies at a specific point in time.
 * <p>
 * This record provides utility methods to perform currency conversion and to derive the inverse
 * (reciprocal) rate. It ensures that all exchange rates are positive and adhere to a standardized
 * FOREX precision.
 * </p>
 *
 * @param from     The source currency being converted from.
 * @param to       The target currency being converted to.
 * @param rate     The multiplier applied to the 'from' currency to get the 'to' currency.
 * @param quotedAt The timestamp when this exchange rate was issued or captured.
 */
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

  /**
   * Creates an identity rate where the source and target currencies are the same.
   *
   * @param currency The currency to represent.
   * @param quotedAt The timestamp for this rate.
   * @return An ExchangeRate with a multiplier of 1.0.
   */
  public static ExchangeRate identity(Currency currency, Instant quotedAt) {
    return new ExchangeRate(currency, currency, BigDecimal.ONE, quotedAt);
  }

  /**
   * Converts a given amount of money into the target currency of this rate.
   *
   * @param money The amount of money in the 'from' currency.
   * @return A new {@link Money} instance in the 'to' currency.
   * @throws CurrencyMismatchException if the money's currency does not match the 'from' currency.
   */
  public Money convert(Money money) {
    notNull(money, "money to convert");

    if (!money.currency().equals(from)) {
      throw new CurrencyMismatchException(money.currency(), from);
    }

    // Use DECIMAL128 for intermediate calculation to maintain high precision
    // before the Money object applies its own internal scaling/rounding rules.
    BigDecimal convertedAmount = money.amount().multiply(rate, MathContext.DECIMAL128);

    return new Money(convertedAmount, to);
  }

  /**
   * Derives the inverse exchange rate (e.g., converts USD/EUR to EUR/USD).
   *
   * @return A new ExchangeRate with swapped currencies and a reciprocal rate.
   */
  public ExchangeRate inverse() {
    return new ExchangeRate(to, from, BigDecimal.ONE.divide(rate, SCALE, ROUNDING), quotedAt);
  }
}