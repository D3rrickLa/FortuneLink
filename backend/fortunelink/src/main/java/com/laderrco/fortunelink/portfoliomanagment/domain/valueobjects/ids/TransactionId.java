package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID transactionId) {
    public TransactionId {
        Objects.requireNonNull(transactionId, "Transaction id cannot be null.");
    }
}
