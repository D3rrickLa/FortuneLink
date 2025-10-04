package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID accountId) {
    public AccountId {
        Objects.nonNull(accountId);
    }

    public static AccountId randomId() {
        return new AccountId(UUID.randomUUID());
    }

}
