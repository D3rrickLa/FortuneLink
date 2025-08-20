package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public record LiabilityPaymentRecordedEvent(
    LiabilityId liabilityId,
    Money principalPaid,
    Money interestPaid,
    Instant occurredAt
) implements DomainEvent {
    public LiabilityPaymentRecordedEvent {
        Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
        Objects.requireNonNull(principalPaid, "Principal payment cannot be null.");
        Objects.requireNonNull(interestPaid, "Interest payment cannot be null.");
        Objects.requireNonNull(occurredAt, "Date of liability payment cannot be null.");
    }
}
