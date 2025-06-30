package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionStatus;

public record TransactionMetadata(TransactionStatus transactionStatus, TransactionSource transactionSource,
        String transactionDescription, Instant createdAt, Instant updatedAt) {
    public TransactionMetadata {
        Objects.requireNonNull(transactionStatus, "Transaction Status cannot be null.");
        Objects.requireNonNull(transactionSource, "Transaction Source cannot be null.");
        Objects.requireNonNull(createdAt, "Creation timestamp cannot be null.");
        Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null.");

    }

    public static TransactionMetadata createInitial(TransactionStatus initialStatus, TransactionSource source,
            String description // Description can be null here if not provided
    ) {
        Objects.requireNonNull(initialStatus, "Initial Transaction Status cannot be null.");
        Objects.requireNonNull(source, "Transaction Source cannot be null for initial creation.");

        Instant now = Instant.now();
        return new TransactionMetadata(initialStatus, source, description, now, now);
    }

    public TransactionMetadata withStatusAndUpdatedAt(TransactionStatus newStatus, Instant newUpdatedAt) {
        Objects.requireNonNull(newStatus, "New Transaction Status cannot be null.");
        Objects.requireNonNull(newUpdatedAt, "New UpdatedAt timestamp cannot be null.");

        // When creating a new version, createdAt must remain the same as the original
        return new TransactionMetadata(newStatus, this.transactionSource, this.transactionDescription, this.createdAt, newUpdatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionMetadata that = (TransactionMetadata) o;
        return Objects.equals(this.transactionStatus, that.transactionStatus)
                && Objects.equals(this.transactionSource, that.transactionSource)
                && Objects.equals(this.transactionDescription, that.transactionDescription)
                && Objects.equals(this.createdAt, that.createdAt)
                && Objects.equals(this.updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.transactionStatus, this.transactionSource, this.transactionDescription, this.createdAt, this.updatedAt);
    }

}