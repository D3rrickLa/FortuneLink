package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;

public record CashflowRecordedEvent(
    PortfolioId portfolioId,
    CashflowTransactionDetails details,
    Money amount, 
    Instant timestamp
) {
    public CashflowRecordedEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(details, "Cashflow details cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(timestamp, "timestamp of event cannot be null.");
    }
}
