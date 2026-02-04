package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

public record TransactionDate(Instant timestamp) implements ClassValidation {

    public TransactionDate {
        ClassValidation.validateParameter(timestamp);
    }

    public static TransactionDate now() {
        return new TransactionDate(Instant.now());
    }

    public boolean isBefore(TransactionDate other) {
        return timestamp.isBefore(other.timestamp());
    }

    public boolean isAfter(TransactionDate other) {
        return timestamp.isAfter(other.timestamp());
    }

    public long toEpochMilli() {
        return timestamp.toEpochMilli();
    }
}