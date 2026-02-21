package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import java.time.Instant;

public record TransactionDate(Instant timestamp) {

    public TransactionDate {
        notNull(timestamp, "timestamp");
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