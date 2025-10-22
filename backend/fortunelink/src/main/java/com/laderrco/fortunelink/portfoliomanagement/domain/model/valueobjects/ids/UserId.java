package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.UUID;

public record UserId(UUID userUuid) implements GenericId {
    public UserId {
        userUuid = GenericId.validate(userUuid);
    }
    public static UserId randomId() {
        return GenericId.random(UserId::new);
    }
}
