package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;

public record PortfolioCreatedEvent(
    PortfolioId portfolioId, 
    UserId userId,
    Money initialBalance,
    Instant timestamp
) {
    public PortfolioCreatedEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(userId, "User id cannot be null.");
        Objects.requireNonNull(initialBalance, "Initial balance cannot be null.");
        Objects.requireNonNull(timestamp, "Timestamp of portfolio creation event cannot be null.");
    }
}
