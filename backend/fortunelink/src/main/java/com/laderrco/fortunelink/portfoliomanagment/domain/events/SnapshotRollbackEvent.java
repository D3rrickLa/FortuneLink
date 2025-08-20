package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public record SnapshotRollbackEvent(
    PortfolioId portfolioId,
    String reason,
    Instant timestamp
) {
    public SnapshotRollbackEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");   
        Objects.requireNonNull(reason, "Reason cannot be null.");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null.");
    }
}
