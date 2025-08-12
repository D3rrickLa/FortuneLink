package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;

public record TransactionReversedEvent(
    PortfolioId portfolioId,
    TransactionId originalTransactionId,
    TransactionId reversalTransactionId,
    Instant timestamp
) {
}
