package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;

public record Fee(
    FeeType type, 
    Money amount,
    String description,
    Instant time
) {
    public Fee {
        validateParameter(type, "constructor - type");
        validateParameter(amount, "constructor - amount");
        validateParameter(description, "constructor - description");
        validateParameter(time, "constructor - time");

        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuantityException("Fee amount must be positive.");
        }
    }
    private void validateParameter(Object other, String methodName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", methodName));
    }

}
