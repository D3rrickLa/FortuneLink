package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.UUID;

public record TransactionId(UUID transactionId) implements GenericId {
    public TransactionId {
        transactionId = GenericId.validate(transactionId);
    }

    public static TransactionId randomId() {
        return GenericId.random(TransactionId::new);
    }
}
