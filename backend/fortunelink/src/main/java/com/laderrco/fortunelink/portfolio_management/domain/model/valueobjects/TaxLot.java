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
    }

    /** Proportion of cost for a partial sale */
    public Money proportionalCost(Quantity soldQuantity) {
        if (soldQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more than lot quantity");
        }

        // Use BigDecimal for precision
        BigDecimal proportion = soldQuantity.amount()
                .divide(quantity.amount(), 10, RoundingMode.HALF_UP);

        return costBasis.multiply(proportion);
    }

    /** Reduce this lot by sold quantity, returns new TaxLot */
    public TaxLot reduce(Quantity soldQuantity) {
        if (soldQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Cannot reduce by more than lot quantity");
        }
        Money newCost = costBasis.subtract(proportionalCost(soldQuantity));
        Quantity newQty = quantity.subtract(soldQuantity);
        return new TaxLot(newQty, newCost, acquiredDate);
    }
}