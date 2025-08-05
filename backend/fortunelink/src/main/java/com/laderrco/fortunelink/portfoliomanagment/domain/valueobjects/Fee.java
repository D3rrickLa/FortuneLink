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
        validateParameter(type, "Type");
        validateParameter(amount, "Amount");
        validateParameter(description, "Description");
        validateParameter(time, "Time");

        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuantityException("Fee amount must be positive.");
        }
    }
    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

}
