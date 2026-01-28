package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.GenericId;

public record AccountId(UUID accountId) implements GenericId {
    public AccountId {
        accountId = GenericId.validate(accountId);
    }
    
    public static AccountId randomId() {
        return GenericId.random(AccountId::new);
    }
}