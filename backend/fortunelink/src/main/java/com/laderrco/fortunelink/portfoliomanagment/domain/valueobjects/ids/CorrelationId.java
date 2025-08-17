package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(UUID correlationId) {
    public CorrelationId {
        Objects.requireNonNull(correlationId, "Correlation id cannot be null.");
    }
}
