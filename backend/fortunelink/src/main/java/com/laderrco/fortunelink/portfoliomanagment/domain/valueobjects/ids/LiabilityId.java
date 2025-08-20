package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record LiabilityId(UUID liabilityId) {
    public LiabilityId {
        Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
    }

    public static LiabilityId createRandom() {
        return new LiabilityId(UUID.randomUUID());
    }
}
