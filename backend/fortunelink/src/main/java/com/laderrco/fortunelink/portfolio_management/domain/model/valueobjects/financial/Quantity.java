package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

public record Quantity(BigDecimal amount) implements ClassValidation, Comparable<Quantity> {
    private static final int QUANTITY_PRECISION = Precision.QUANTITY.getDecimalPlaces();
    private static final RoundingMode Q_ROUNDING_MODE = Rounding.QUANTITY.getMode();

    public Quantity {
        ClassValidation.validateParameter(amount, "amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
        }
        amount = amount.setScale(QUANTITY_PRECISION, RoundingMode.HALF_EVEN);
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
        return new Quantity(this.amount.multiply(factor).setScale(QUANTITY_PRECISION, RoundingMode.HALF_EVEN));
    }

    public Quantity divide(BigDecimal divisor) {
        ClassValidation.validateParameter(divisor, "factor");
        if (divisor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Divisor cannot be negative");
        }
        return new Quantity(this.amount.divide(divisor, QUANTITY_PRECISION, Q_ROUNDING_MODE));
    }

    @Override
    public int compareTo(Quantity other) {
        return this.amount.compareTo(other.amount());
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNonZero() {
        return !isZero();
    }

    public Quantity abs() {
        return new Quantity(this.amount.abs());
    }
}
