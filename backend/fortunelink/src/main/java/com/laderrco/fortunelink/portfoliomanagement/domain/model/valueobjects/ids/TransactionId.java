package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID transactionId) {
    public TransactionId {
        Objects.nonNull(transactionId);
    }

    public static TransactionId randomId() {
        return new TransactionId(UUID.randomUUID());
    }

}
