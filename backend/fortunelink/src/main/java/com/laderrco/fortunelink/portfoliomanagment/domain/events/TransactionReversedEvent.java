package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;

public record TransactionReversedEvent(
    PortfolioId portfolioId,
    TransactionId originalTransactionId,
    TransactionId reversalTransactionId,
    Instant timestamp
) {
    public TransactionReversedEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(originalTransactionId, "Original transaction id cannot be null.");
        Objects.requireNonNull(reversalTransactionId, "Reversal transaction id cannot be null.");
        Objects.requireNonNull(timestamp, "Timestamp of reversal cannot be null.");

    }
}
