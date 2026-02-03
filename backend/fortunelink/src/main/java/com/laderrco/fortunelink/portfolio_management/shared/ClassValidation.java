package com.laderrco.fortunelink.portfolio_management.shared;

import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.DomainArgumentException;

public interface ClassValidation {
    static <T> T validateParameter(T value) {
        return Objects.requireNonNull(value, "Parameter cannot be null");
    }

    static <T> T validateParameter(T value, String name) {
        // Benefit: You can add more checks here later, like 
        // blank string checks or range validation, without changing the X record.
        if (value == null) {
            throw new DomainArgumentException(String.format("%s is required and cannot be null", name));
        }
        return value;
    }
}