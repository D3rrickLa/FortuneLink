package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.UUID;

public record AccountId(UUID accountId) implements GenericId {
    public AccountId {
        accountId = GenericId.validate(accountId);
    }
    
    public static AccountId randomId() {
        return GenericId.random(AccountId::new);
    }
}
