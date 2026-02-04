package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers;

import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.shared.enums.GenericId;

public record UserId(UUID id) implements GenericId {
    public UserId {
        GenericId.validate(id);
    }

    public static UserId random() {
        return GenericId.generate(UserId::new);
    }

    public static UserId fromString(String value) {
        return GenericId.fromString(UserId::new, value);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}