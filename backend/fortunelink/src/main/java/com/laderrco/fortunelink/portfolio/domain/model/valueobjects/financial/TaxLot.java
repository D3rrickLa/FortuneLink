package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Represents a single batch of an asset acquired at a specific time and cost.
 * <p>
 * This record maintains the history of a tax lot through partial sales and stock splits.
 * <b>Note on Cost Basis:</b> The {@code costBasis} represents the total amount paid for the
 * entire lot. The average price per share (PPS) is derived as {@code costBasis / quantity}.
 */
public record TaxLot(Quantity quantity, Money costBasis, Instant acquiredDate) {
  private static final int MONEY_PRECISION = Precision.getMoneyPrecision();
  private static final RoundingMode M_ROUNDING_MODE = Rounding.MONEY.getMode();

  public TaxLot {
    notNull(quantity, "quantity");
    notNull(costBasis, "costBasis");
    notNull(acquiredDate, "acquiredDate");

    if (costBasis.isNegative()) {
      throw new IllegalArgumentException("TaxLot cost basis cannot be negative: " + costBasis);
    }
  }

  /**
   * Calculates the portion of the cost basis to realize for a partial sale.
   * <p>
   * <b>Example:</b> If you hold 10 shares with a $1000 cost basis and sell 4 shares,
   * this calculates the 40% pro rata cost: {@code 4/10 * $1000 = $400}.
   *
   * @param soldQuantity The number of shares being sold.
   * @return The portion of the cost basis associated with the sold quantity.
   */
  public Money proportionalCost(Quantity soldQuantity) {
    if (soldQuantity.compareTo(quantity) > 0) {
      throw new IllegalArgumentException("Cannot sell more than lot quantity");
    }

    if (soldQuantity.isZero()) {
      return Money.ZERO(costBasis.currency());
    }

    BigDecimal proportion = soldQuantity.amount()
        .divide(quantity.amount(), MONEY_PRECISION, M_ROUNDING_MODE);

    return costBasis.multiply(proportion);
  }

  /**
   * Returns a new {@code TaxLot} representing the remainder of this lot after a sale.
   * <p>
   * <b>Example:</b> Selling 4 shares from a 10-share/ $1000-cost-basis lot results
   * in a new lot: {@code qty = 6, costBasis = $600, acquiredDate = original}.
   *
   * @param soldQuantity The number of shares removed from this lot.
   * @return A new {@link TaxLot} instance with reduced quantity and cost basis.
   */
  public TaxLot remainingAfter(Quantity soldQuantity) {
    if (soldQuantity.compareTo(quantity) > 0) {
      throw new IllegalArgumentException(
          String.format("Cannot reduce lot by %s when it only has %s", soldQuantity, quantity));
    }

    if (soldQuantity.isZero()) {
      return this; // No change
    }

    Money soldCost = proportionalCost(soldQuantity);
    Money newCost = costBasis.subtract(soldCost);
    Quantity newQty = quantity.subtract(soldQuantity);

    return new TaxLot(newQty, newCost, acquiredDate);
  }

  /**
   * Adjusts this lot to account for a stock split.
   *
   * @param ratio The split ratio (e.g., 2:1 ratio for a 2-for-1 split).
   * @return A new {@link TaxLot} with adjusted quantity, maintaining the total cost basis.
   * @implNote We might have an issue with accuracy. If we have any rounding/precision loss during
   * the newQuantity calculation, the 'cost basis per share' will drift. If BigDecimal doesn't have
   * the same scale as your Money object, you might lose pennies.
   */
  public TaxLot split(Ratio ratio) {
    BigDecimal factor = ratio.multiplier();
    Quantity newQuantity = this.quantity.multiply(factor)
        .setScale(Precision.QUANTITY.getDecimalPlaces(), Rounding.QUANTITY.getMode());

    // Total cost basis for the lot remains the same
    return new TaxLot(newQuantity, costBasis, acquiredDate);
  }

  /**
   * Returns the number of days between acquisition and sale.
   * <p>
   * Useful for determining long-term vs short-term capital gains.
   */
  public long getHoldingPeriodDays(Instant saleDate) {
    return Duration.between(acquiredDate, saleDate).toDays();
  }

  /**
   * Determines if the lot qualifies for long-term capital gains tax treatment (365+ days).
   */
  public boolean isLongTerm(Instant saleDate) {
    return getHoldingPeriodDays(saleDate) >= 365;
  }
}