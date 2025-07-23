package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.exceptions.InvalidQuantityException;

public record Fee(FeeType feeType, Money amount) {
    public Fee {
        Objects.requireNonNull(feeType, "Fee type cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");

        if (amount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidQuantityException("Fee amount cannot be negative.");
        }
    }
    
}
