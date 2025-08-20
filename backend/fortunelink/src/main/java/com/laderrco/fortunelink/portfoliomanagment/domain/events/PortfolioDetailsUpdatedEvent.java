package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public record PortfolioDetailsUpdatedEvent(
    PortfolioId portfolioId,
    String oldName,
    String newName,
    String oldDescription,
    String newDescription,
    Instant occurredAt
) implements DomainEvent {
    public PortfolioDetailsUpdatedEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(oldName, "Old name cannot be null.");
        Objects.requireNonNull(newName, "New name cannot be null.");
        Objects.requireNonNull(oldDescription, "Old description cannot be null.");
        Objects.requireNonNull(newDescription, "New description cannot be null.");
        Objects.requireNonNull(occurredAt, "Occurred at cannot be null.");
    }
}
