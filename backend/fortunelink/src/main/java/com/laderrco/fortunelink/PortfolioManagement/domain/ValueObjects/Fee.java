package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public record Fee(FeeType feeType, Money feeAmount) {
    public Fee {
        Objects.requireNonNull(feeType, "Fee type cannot be null.");
        Objects.requireNonNull(feeAmount, "Fee amount cannot be null.");

        if (feeAmount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amount cannot be a negative value.");
        }
    }
}