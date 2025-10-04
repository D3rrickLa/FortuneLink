package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(UUID correlationId) {
    public CorrelationId {
        Objects.nonNull(correlationId);
    }

    public static CorrelationId randomId() {
        return new CorrelationId(UUID.randomUUID());
    }

}
