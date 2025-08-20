package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public record SnapshotRollbackEvent(
    PortfolioId portfolioId,
    String reason,
    Instant occurredAt
) implements DomainEvent {
    public SnapshotRollbackEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");   
        Objects.requireNonNull(reason, "Reason cannot be null.");
        Objects.requireNonNull(occurredAt, "Occured at cannot be null.");
    }
}
