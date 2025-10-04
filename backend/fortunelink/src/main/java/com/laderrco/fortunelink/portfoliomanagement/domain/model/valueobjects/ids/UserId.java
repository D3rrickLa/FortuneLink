package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID userId) {
    public UserId {
        Objects.nonNull(userId);
    }

    public static UserId randomId() {
        return new UserId(UUID.randomUUID());
    }

}
