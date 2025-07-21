package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import java.util.UUID;

public final class ReversalTransactionDetails extends TransactionDetails {
    private final UUID originalTransactionId;
    private final String reason;

    public ReversalTransactionDetails(UUID originalTransactionId, String reason) {
        this.originalTransactionId = originalTransactionId;
        this.reason = reason;
    }
    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }
    public String getReason() {
        return reason;
    }

    
}
