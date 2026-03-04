package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

/**
 * Preserves history, i.e.:
 * Lot 1: 10 shares @ $100 (Jan 1)
 * Lot 2: 5 shares @ $120 (Feb 2)
 * <p>
 * NOTE ON COSTBASIS: it is the 'pps * units' FOR EACH TRANSACTION
 * so the example above would be $1000 + $600 = $1600 is our cost basis
 * the avg pps is $106.67 or cost basis / # of shares
 */
public record TaxLot(Quantity quantity, Money costBasis, Instant acquiredDate) {

    private static final int MONEY_PRECISION = Precision.getMoneyPrecision();
    private static final RoundingMode M_ROUNDING_MODE = Rounding.MONEY.getMode();

    public TaxLot {
        notNull(quantity, "quantity");
        notNull(costBasis, "costBasis");
        notNull(acquiredDate, "acquiredDate");

        if (quantity.isNegative()) {
            throw new IllegalArgumentException("TaxLot quantity cannot be negative: " + quantity);
        }

        if (costBasis.isNegative()) {
            throw new IllegalArgumentException("TaxLot cost basis cannot be negative: " + costBasis);
        }
    }

    /**
     * Proportion of cost for a partial sale
     * <p>
     * If we had 10 shares for $1000
     * <p>
     * and we sell 4 shares, how much of the OG $1000
     * cost basis should be 'used up/ sold'?
     * <p>
     * ANSWER: 4/10 = 40% and 40% of $1000 = $400. That is what we are doing here
     */
    public Money proportionalCost(Quantity soldQuantity) {
        if (soldQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more than lot quantity");
        }

        if (soldQuantity.isNegative()) {
            throw new IllegalArgumentException("Sold quantity cannot be negative: " + soldQuantity);
        }

        if (soldQuantity.isZero()) {
            return Money.ZERO(costBasis.currency());
        }

        BigDecimal proportion = soldQuantity.amount().divide(quantity.amount(), MONEY_PRECISION, M_ROUNDING_MODE);

        return costBasis.multiply(proportion);
    }

    /**
     * Reduce this lot by sold quantity, returns new TaxLot
     * <p>
     * We are returning the aftermath of the 'selling'
     * If we had 10 shares for $1000 and we sell 4 shares, we return :
     * TaxLot(
     * qty = 6,
     * costBasis = $600,
     * acquiredDate = Jan 1
     * )
     */
    public TaxLot remainingAfter(Quantity soldQuantity) {
        if (soldQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException(String.format("Cannot reduce lot by %s when it only has %s",
                    soldQuantity, quantity));
        }

        if (soldQuantity.isNegative()) {
            throw new IllegalArgumentException("Sold quantity cannot be negative: " + soldQuantity);
        }

        if (soldQuantity.isZero()) {
            return this; // No change
        }

        Money soldCost = proportionalCost(soldQuantity);
        Money newCost = costBasis.subtract(soldCost);
        Quantity newQty = quantity.subtract(soldQuantity);

        return new TaxLot(newQty, newCost, acquiredDate);
    }

    public TaxLot split(Ratio ratio) {
        Quantity newQuantity = this.quantity
                .multiply(BigDecimal.valueOf(ratio.numerator()))
                .divide(BigDecimal.valueOf(ratio.denominator()));

        return new TaxLot(
                newQuantity,
                costBasis, // Total cost basis for the lot remains the same
                acquiredDate);
    }

    /**
     * Calculate the holding period in days.
     * Useful for determining long-term vs short-term capital gains.
     */
    public long getHoldingPeriodDays(Instant saleDate) {
        return java.time.Duration.between(acquiredDate, saleDate).toDays();
    }

    /**
     * Check if this lot qualifies for long-term capital gains treatment.
     * In most jurisdictions, this is 365+ days.
     */
    public boolean isLongTerm(Instant saleDate) {
        return getHoldingPeriodDays(saleDate) >= 365;
    }
}