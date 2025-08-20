package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;

public record LiabilityUpdatedEvent(
    LiabilityId liabilityId,
    LiabilityDetails oldDetails,
    LiabilityDetails newDetails,
    Instant occurredAt
) implements DomainEvent{
    public LiabilityUpdatedEvent {
        Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
        Objects.requireNonNull(oldDetails, "Old details cannot be null.");
        Objects.requireNonNull(newDetails, "New details cannot be null.");
        Objects.requireNonNull(occurredAt, "Occurred at cannot be null.");
    }
}
