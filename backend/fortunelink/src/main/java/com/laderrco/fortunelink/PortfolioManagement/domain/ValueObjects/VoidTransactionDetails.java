package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Objects;
import java.util.UUID;

public class VoidTransactionDetails extends TransactionDetails {
    private final UUID originalTransactionId;
    private final String reason;

    public VoidTransactionDetails(UUID originalTransactionId, String reason) {
        Objects.requireNonNull(originalTransactionId, "Original Transaction ID cannot be null.");
        Objects.requireNonNull(reason, "Reason for voiding cannot be null.");

        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is empty, please enter a reason for voiding the transaction.");
        }

        this.originalTransactionId = originalTransactionId;
        this.reason = reason;
    }

    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VoidTransactionDetails that = (VoidTransactionDetails) o;
        return Objects.equals(this.originalTransactionId, that.originalTransactionId) 
                && Objects.equals(this.reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.originalTransactionId, this.reason);

    }

}
