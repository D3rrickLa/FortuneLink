package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.portfolio_management.domain.model.ClassValidation;

public record Quantity(BigDecimal amount) implements ClassValidation {
    // TODO add a better scale factor in @Link{Precision.java}
    public Quantity {
        ClassValidation.validateParameter(amount, "amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
        }
        // Optional: scale if needed for fractional units
        amount = amount.setScale(8, RoundingMode.HALF_EVEN);
    }

    public static final Quantity ZERO = new Quantity(BigDecimal.ZERO);

    public Quantity add(Quantity other) {
        ClassValidation.validateParameter(other, "other quantity");
        return new Quantity(this.amount.add(other.amount));
    }

    public Quantity subtract(Quantity other) {
        ClassValidation.validateParameter(other, "other quantity");
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Resulting quantity cannot be negative");
        }
        return new Quantity(result);
    }

    public Quantity multiply(BigDecimal factor) {
        ClassValidation.validateParameter(factor, "factor");
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Factor cannot be negative");
        }
        return new Quantity(this.amount.multiply(factor).setScale(8, RoundingMode.HALF_EVEN));
    }
}