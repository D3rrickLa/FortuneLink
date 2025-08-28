
package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID transactionId) {
    public TransactionId {
        Objects.requireNonNull(transactionId, "Transaction id cannot be null.");
    }

    public static TransactionId createRandom() {
        return new TransactionId(UUID.randomUUID());
    }
}