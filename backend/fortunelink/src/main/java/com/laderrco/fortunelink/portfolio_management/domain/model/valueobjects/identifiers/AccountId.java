package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers;

import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.shared.enums.GenericId;

public record AccountId(UUID id) implements GenericId {

    public AccountId {
        GenericId.validate(id);
    }

    public static AccountId newId() {
        return GenericId.generate(AccountId::new);
    }

    public static AccountId fromString(String value) {
        return GenericId.fromString(AccountId::new, value);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}