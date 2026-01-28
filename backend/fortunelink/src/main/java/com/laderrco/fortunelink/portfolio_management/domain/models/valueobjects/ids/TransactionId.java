package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.GenericId;

public record TransactionId(UUID transactionId) implements GenericId {
    public TransactionId {
        transactionId = GenericId.validate(transactionId);
    }

    public static TransactionId randomId() {
        return GenericId.random(TransactionId::new);
    }
}