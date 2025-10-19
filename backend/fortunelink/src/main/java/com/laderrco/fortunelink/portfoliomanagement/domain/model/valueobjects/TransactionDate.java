package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.util.Objects;

public record TransactionDate(LocalDateTime timestamp) {
    public TransactionDate {
        Objects.requireNonNull(timestamp, "Transaction date cannot be null");
    }

    public boolean isBefore(TransactionDate date) {
        return this.timestamp.isBefore(date.timestamp());
    }
    public boolean isAfter(TransactionDate date) {
        return this.timestamp.isAfter(date.timestamp());
    }
}
