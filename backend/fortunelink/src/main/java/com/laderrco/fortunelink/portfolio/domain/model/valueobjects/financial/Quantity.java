package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

public record Quantity(BigDecimal amount) implements Comparable<Quantity> {
    private static final int QUANTITY_PRECISION = Precision.QUANTITY.getDecimalPlaces();
    private static final RoundingMode Q_ROUNDING_MODE = Rounding.QUANTITY.getMode();

    public Quantity {
        notNull(amount, "amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + amount);
        }
        amount = amount.setScale(QUANTITY_PRECISION, RoundingMode.HALF_EVEN);
    }

    public static final Quantity ZERO = new Quantity(BigDecimal.ZERO);

    public static Quantity of(double i) {
        return new Quantity(BigDecimal.valueOf(i));
    }

    public Quantity add(Quantity other) {
        notNull(other, "other quantity");
        return new Quantity(this.amount.add(other.amount));
    }

    public Quantity subtract(Quantity other) {
        notNull(other, "other quantity");
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Resulting quantity cannot be negative");
        }
        return new Quantity(result);
    }

    public Quantity multiply(BigDecimal factor) {
        notNull(factor, "factor");
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Factor cannot be negative");
        }
        return new Quantity(
                this.amount.multiply(factor).setScale(QUANTITY_PRECISION, RoundingMode.HALF_EVEN));
    }

    public Quantity divide(BigDecimal divisor) {
        notNull(divisor, "factor");
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Divisor must be positive");
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

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNonZero() {
        return !isZero();
    }
}
