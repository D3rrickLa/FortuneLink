package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.GenericId;

// we shouldn't validate if userId is null as there are times when it needs to be null
// mainly 'deleteBy' in portfolio
public record UserId(UUID userId) implements GenericId {
    public UserId {    }

    public static UserId randomId() {
        return GenericId.random(UserId::new);
    }
    
}
