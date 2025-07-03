
package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;

import org.springframework.transaction.TransactionStatus;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionSource;

public record TransactionMetadata(TransactionStatus transactionStatus, TransactionSource transactionSource, String transactionDescription, Instant createdAt, Instant updatedAt) {
    public TransactionMetadata {
        Objects.requireNonNull(transactionStatus, "Transaction Status cannot be null.");
        Objects.requireNonNull(transactionSource, "Transaction Source cannot be null.");
        Objects.requireNonNull(createdAt, "Creation timestamp cannot be null.");
        Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null.");
    }

    public static TransactionMetadata createMetadata(TransactionStatus initialStatus, TransactionSource source, String description) {
        return new TransactionMetadata(initialStatus, source, description, Instant.now(), Instant.now());
    }

    public TransactionMetadata updateStatus(TransactionStatus newStatus, Instant newUpdatedAt) {
        Objects.requireNonNull(newStatus, "New Transaction Status cannot be null.");
        Objects.requireNonNull(newUpdatedAt, "New UpdatedAt timestamp cannot be null.");
        return new TransactionMetadata(newStatus, this.transactionSource, this.transactionDescription, this.createdAt, newUpdatedAt);
    }
}