package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidQuantityException;

/**
 * Encapsualtes both native and portfolio values
 */
public record MonetaryAmount(
    Money nativeAmount,
    CurrencyConversion conversion
) {
    public MonetaryAmount {
        nativeAmount = Objects.requireNonNull(nativeAmount, "nativeAmount cannot be null");
        conversion = Objects.requireNonNull(conversion, "conversion cannot be null");
        if (nativeAmount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidQuantityException("Amount cannot be negative.");
        }
    }

    public static final MonetaryAmount ZERO() {
        return new MonetaryAmount(null, null)
    }
    
    public MonetaryAmount add(MonetaryAmount other) {
        return new MonetaryAmount(this.nativeAmount.add(other.nativeAmount), this.conversion);
    }

    public MonetaryAmount multiply(BigDecimal factor) {
        return new MonetaryAmount(this.nativeAmount.multiply(factor), this.conversion);
    }

    public Money getPortfolioAmount() {
        return conversion.convert(this.nativeAmount);
    }
}
