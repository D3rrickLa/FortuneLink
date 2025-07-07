package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;

public record TransactionMetadata(
    TransactionStatus transactionStatus, 
    TransactionSource transactionSource,
    String description,
    Instant createdAt,
    Instant updatedAt
) {
    public TransactionMetadata {
        Objects.requireNonNull(transactionStatus, "Transaction status cannot be null.");
        Objects.requireNonNull(transactionSource, "Transaction source cannot be null.");
        Objects.requireNonNull(createdAt, "Creation timestamp cannot be null.");
        Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null.");
    }


    public static TransactionMetadata createMetadata(
        TransactionStatus initialStatus, 
        TransactionSource source, 
        String description, 
        Instant createdAt
    ) {
        description = description.trim();
        return new TransactionMetadata(initialStatus, source, description, createdAt, createdAt);
    }

    public TransactionMetadata updateStatus(TransactionStatus newStatus) {
        return new TransactionMetadata(newStatus, this.transactionSource, this.description, this.createdAt, Instant.now());
    }
}
