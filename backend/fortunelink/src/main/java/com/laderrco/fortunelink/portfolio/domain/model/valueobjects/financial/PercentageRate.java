package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Represents a non-negative percentage rate (e.g., 0.05 for 5%).
 * <p>
 * This record ensures consistent precision and rounding for financial calculations. It supports
 * both decimal rates and 0-100 percentage values through static factory methods.
 * </p>
 *
 * @param rate The decimal representation of the percentage (e.g., 0.05 represents 5%).
 */
public record PercentageRate(BigDecimal rate) implements Comparable<PercentageRate> {
  private static final RoundingMode P_ROUNDING_MODE = Rounding.PERCENTAGE.getMode();
  private static final int RATE_PRECISION = Precision.PERCENTAGE.getDecimalPlaces();

  /**
   * Compact constructor. Validates that the rate is non-null and non-negative, then scales it to
   * the standard {@code RATE_PRECISION}.
   *
   * @throws IllegalArgumentException if rate is negative.
   * @throws NullPointerException     if rate is null.
   */
  public PercentageRate {
    notNull(rate, "rate");

    if (rate.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("PercentageRate rate cannot be negative");
    }

    rate = rate.setScale(RATE_PRECISION, P_ROUNDING_MODE);
  }

  /**
   * Creates a PercentageRate from a decimal rate.
   *
   * @param rate The decimal rate (e.g., {@code 0.05} for 5%).
   * @return A new PercentageRate instance.
   */
  public static PercentageRate fromRate(BigDecimal rate) {
    return new PercentageRate(rate);
  }

  /**
   * Creates a PercentageRate from a percentage value.
   *
   * @param PercentageRate The percentage value (e.g., {@code 5.0} for 5%).
   * @return A new PercentageRate instance representing the rate (0.05).
   */
  public static PercentageRate fromPercent(BigDecimal PercentageRate) {
    return new PercentageRate(
        PercentageRate.divide(BigDecimal.valueOf(100), RATE_PRECISION, P_ROUNDING_MODE));
  }

  /**
   * Converts the internal rate back to a percentage value.
   *
   * @return The rate multiplied by 100 (e.g., 0.05 returns 5.0).
   */
  public BigDecimal toPercent() {
    return rate.multiply(BigDecimal.valueOf(100));
  }

  /**
   * Adds another percentage rate to this one.
   */
  public PercentageRate add(PercentageRate other) {
    return new PercentageRate(rate.add(other.rate));
  }

  /**
   * Compounds this rate with another.
   */
  public PercentageRate compoundWith(PercentageRate other) {
    return new PercentageRate(rate.multiply(other.rate));
  }

  /**
   * Calculates the annualized rate over a given number of years.
   * <p>Uses the formula: $((1 + \text{rate})^{1/\text{years}}) - 1$</p>
   *
   * @param years The period in years; must be greater than zero.
   * @return The annualized PercentageRate.
   * @throws IllegalArgumentException if years is not positive.
   */
  public PercentageRate annualizedOver(BigDecimal years) {
    if (years.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Years must be positive");
    }
    /*
     * BigDecimal does not support fractional exponentiation.
     * We temporarily convert to double for Math.pow().
     *
     * This introduces minor floating-point rounding error, but the magnitude
     * is tiny for typical financial return values and acceptable
     * for performance/return display purposes.
     *
     * If higher precision is required in the future, replace with a
     * BigDecimal Newton-method implementation or a library such as
     * BigDecimalMath / Apache Commons Math.
     */
    @SuppressWarnings("FloatingPointLiteralPrecision") double annualized =
        Math.pow(BigDecimal.ONE.add(rate).doubleValue(),
            BigDecimal.ONE.divide(years, MathContext.DECIMAL64).doubleValue()) - 1.0;

    return new PercentageRate(BigDecimal.valueOf(annualized));
  }

  @Override
  public int compareTo(PercentageRate other) {
    return rate.compareTo(other.rate);
  }
}