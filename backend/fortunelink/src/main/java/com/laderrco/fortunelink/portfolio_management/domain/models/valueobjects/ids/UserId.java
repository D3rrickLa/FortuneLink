package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.GenericId;

public record UserId(UUID userId) implements GenericId {
    public UserId {
        userId = GenericId.validate(userId);
    }

    public static UserId randomId() {
        return GenericId.random(UserId::new);
    }
    
}
