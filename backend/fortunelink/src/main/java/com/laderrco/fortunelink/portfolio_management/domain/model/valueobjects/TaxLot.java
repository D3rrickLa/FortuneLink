package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects;

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

    /** Calculates gain for selling a portion of this lot at given price */
    public Money calculateGain(Money sellPrice, Quantity sellQuantity) {
        if (sellQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more than lot quantity");
        }

        // Proportional gain
        Money costPortion = costBasis.multiply(sellQuantity.divide(quantity.amount()).amount());
        Money proceeds = sellPrice.multiply(sellQuantity.amount());

        return proceeds.subtract(costPortion);
    }

    public Money proportionalCost(Quantity sold) {
        return costBasis.multiply(sold.divide(quantity.amount()).amount());
    }
}