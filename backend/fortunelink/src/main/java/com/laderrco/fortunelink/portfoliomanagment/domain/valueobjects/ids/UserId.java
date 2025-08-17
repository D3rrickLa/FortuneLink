package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID userId) {
    public UserId {
        Objects.requireNonNull(userId, "User id cannot be null.");
    }
}
