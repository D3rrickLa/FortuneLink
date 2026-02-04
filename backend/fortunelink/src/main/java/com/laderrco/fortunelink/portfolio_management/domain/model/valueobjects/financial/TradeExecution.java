package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// know what was traded, not the account who paid for it
public record TradeExecution(AssetSymbol asset, Quantity quantity, Money pricePerUnit) {
    public TradeExecution {
        ClassValidation.validateParameter(asset, "Asset symbol cannot be null");
        ClassValidation.validateParameter(quantity, "Quantity cannot be null");
        ClassValidation.validateParameter(pricePerUnit, "Price per unit cannot be null");

        // Domain validation
        if (quantity.isZero()) {
            throw new IllegalArgumentException("Trade quantity cannot be zero");
        }

        if (pricePerUnit.isNegative()) {
            throw new IllegalArgumentException(
                    "Price per unit cannot be negative (got: " + pricePerUnit + ")");
        }
    }

    /**
     * Gross value of the trade before fees.
     * This is quantity × price, representing the market value.
     */
    public Money grossValue() {
        return pricePerUnit.multiply(quantity.amount().abs());
    }
}