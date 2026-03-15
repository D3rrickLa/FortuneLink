package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;

/**
 * Represents a mathematical ratio used for asset adjustments such as stock splits.
 * <p>
 * This ratio is defined by a {@code numerator} and {@code denominator}, representing a factor of
 * {@code numerator / denominator}.
 * <ul>
 * <li>A 3-for-1 split is represented as {@code (3, 1)}.</li>
 * <li>A 1-for-10 reverse split is represented as {@code (1, 10)}.</li>
 * </ul>
 */
public record Ratio(int numerator, int denominator) {
  public Ratio {
    if (numerator <= 0) {
      throw new IllegalArgumentException("Numerator must be greater than zero");
    }
    if (denominator <= 0) {
      throw new IllegalArgumentException("Denominator must be greater than zero");
    }
  }

  public BigDecimal multiplier() {
    return BigDecimal.valueOf(numerator).divide(
        BigDecimal.valueOf(denominator),
        Precision.DIVISION.getDecimalPlaces(), Rounding.DIVISION.getMode()
    );
  }
}
