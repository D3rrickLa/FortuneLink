package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

public record TaxLot(Quantity quantity, Money costBasis, Instant acquiredDate) implements ClassValidation {

    public TaxLot {
        ClassValidation.validateParameter(quantity);
        ClassValidation.validateParameter(costBasis);
        ClassValidation.validateParameter(acquiredDate);

        if (quantity.isNegative()) {
            throw new IllegalArgumentException("TaxLot quantity cannot be negative: " + quantity);
        }

        if (costBasis.isNegative()) {
            throw new IllegalArgumentException("TaxLot cost basis cannot be negative: " + costBasis);
        }
    }

    /** Proportion of cost for a partial sale */
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

        // TODO impl your own scaling
        BigDecimal proportion = soldQuantity.amount()
                .divide(quantity.amount(), 10, RoundingMode.HALF_UP);

        return costBasis.multiply(proportion);
    }

    /** Reduce this lot by sold quantity, returns new TaxLot */
    public TaxLot reduce(Quantity soldQuantity) {
        if (soldQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot reduce lot by %s when it only has %s",
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