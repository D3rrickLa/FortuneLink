package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;

public record PortfolioCreatedEvent(
    PortfolioId portfolioId, 
    UserId userId,
    Money initialBalance,
    Instant occurredAt
) implements DomainEvent {
    public PortfolioCreatedEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(userId, "User id cannot be null.");
        Objects.requireNonNull(initialBalance, "Initial balance cannot be null.");
        Objects.requireNonNull(occurredAt, "Occurrence of portfolio creation event cannot be null.");
    }
}
