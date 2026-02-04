package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers;

import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.shared.enums.GenericId;

public record TransactionId(UUID id) implements GenericId {
    public TransactionId {
        GenericId.validate(id);
    }

    public static TransactionId newId() {
        return GenericId.generate(TransactionId::new);
    }

    public static TransactionId fromString(String value) {
        return GenericId.fromString(TransactionId::new, value);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}