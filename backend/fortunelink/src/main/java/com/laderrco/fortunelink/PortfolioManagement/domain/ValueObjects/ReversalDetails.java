package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Objects;
import java.util.UUID;

public class ReversalDetails extends TransactionDetails {
        private final UUID originalTransactionId;
    private final String reasonForReversal;

    public ReversalDetails(UUID originalTransactionId, String reasonForReversal) {
        Objects.requireNonNull(originalTransactionId, "Original Transaction ID for reversal cannot be null.");
        Objects.requireNonNull(reasonForReversal, "Reason for reversal cannot be null.");
        if (reasonForReversal.isBlank()) {
            throw new IllegalArgumentException("Reason for reversal cannot be blank.");
        }
        this.originalTransactionId = originalTransactionId;
        this.reasonForReversal = reasonForReversal;
    }

    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }

    public String getReasonForReversal() {
        return reasonForReversal;
    }
}
