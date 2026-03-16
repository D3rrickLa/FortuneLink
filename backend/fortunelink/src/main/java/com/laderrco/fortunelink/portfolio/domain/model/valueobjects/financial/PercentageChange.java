package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents a relative change in value, which can be positive (gain) or negative (loss).
 * <p>
 * This record is used for performance metrics such as returns, growth rates, and period-over-period
 * comparisons (e.g., YTD, MOM).
 * </p>
 * * <p> Values are stored internally as a decimal rate (e.g., {@code 0.15} for +15%, {@code -0.25}
 * for -25%).</p>
 *
 * @param change The decimal representation of the percentage change.
 */
public record PercentageChange(BigDecimal change) implements Comparable<PercentageChange> {
  public static final PercentageChange ZERO = new PercentageChange(BigDecimal.ZERO);
  private static final int SCALE = Precision.PERCENTAGE.getDecimalPlaces();
  private static final RoundingMode MODE = Rounding.PERCENTAGE.getMode();

  public PercentageChange {
    notNull(change, "percentage change");
    change = change.setScale(SCALE, MODE);
  }

  /**
   * Static factory to create a gain from a positive percentage number.
   *
   * @param percent The percentage value to be treated as a gain (e.g. {@code 10.0} creates 0.10).
   * @return A new PercentageChange instance representing a positive rate.
   */
  public static PercentageChange gain(double percent) {
    return new PercentageChange(
        BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), SCALE, MODE));
  }

  /**
   * Static factory to create a loss from a positive percentage number.
   *
   * @param percent The percentage value to be treated as a loss (e.g. {@code 20.0} creates -0.20).
   * @return A new PercentageChange instance representing a negative rate.
   */
  public static PercentageChange loss(double percent) {
    return new PercentageChange(
        BigDecimal.valueOf(percent).negate().divide(BigDecimal.valueOf(100), SCALE, MODE));
  }

  /**
   * Converts the internal decimal rate back to a percentage value. * @return The change multiplied
   * by 100 (e.g., -0.25 returns -25.0).
   */
  public BigDecimal toPercent() {
    return change.multiply(BigDecimal.valueOf(100));
  }

  /**
   * Checks if the change represents a positive return.
   *
   * @return true if the rate is greater than zero.
   */
  public boolean isGain() {
    return change.signum() > 0;
  }

  /**
   * Checks if the change represents a negative return.
   *
   * @return true if the rate is less than zero.
   */
  public boolean isLoss() {
    return change.signum() < 0;
  }

  @Override
  public int compareTo(PercentageChange other) {
    return change.compareTo(other.change);
  }
}