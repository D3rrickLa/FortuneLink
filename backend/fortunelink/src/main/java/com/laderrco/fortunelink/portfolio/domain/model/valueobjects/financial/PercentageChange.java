package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents a percentage change - can be positive or negative. Used for returns, gains, losses,
 * growth rates.
 * <p>
 * Examples: -25.5%, +150%, -99.9%, -20% YTD or +50% MOM
 */
public record PercentageChange(BigDecimal change) implements Comparable<PercentageChange> {
  public static final PercentageChange ZERO = new PercentageChange(BigDecimal.ZERO);
  private static final int SCALE = Precision.PERCENTAGE.getDecimalPlaces();
  private static final RoundingMode MODE = Rounding.PERCENTAGE.getMode();

  public PercentageChange {
    notNull(change, "percentage change");
    change = change.setScale(SCALE, MODE);
  }

  // -0.20
  public static PercentageChange loss(double percent) {
    return new PercentageChange(
        BigDecimal.valueOf(percent).negate().divide(BigDecimal.valueOf(100), SCALE, MODE));
  }

  // +0.10
  public static PercentageChange gain(double percent) {
    return new PercentageChange(
        BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), SCALE, MODE));
  }

  public BigDecimal toPercent() {
    return change.multiply(BigDecimal.valueOf(100));
  }

  public boolean isLoss() {
    return change.signum() < 0;
  }

  public boolean isGain() {
    return change.signum() > 0;
  }

  @Override
  public int compareTo(PercentageChange other) {
    return change.compareTo(other.change);
  }
}