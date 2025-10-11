package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;

public record DividendReceivedEvent(PortfolioId portfolioId, AssetHoldingId assetHoldingId, Money dividendAmount, Instant transactionDate) implements DomainEvent {

    @Override
    public Instant occuredOn() {
        return this.transactionDate;
    }

    @Override
    public String eventType() {
        return "Dividend received event.";
    }

    @Override
    public String aggregateId() {
        return String.format("{PortfolioId: %s, AssetHoldingId: %s}", this.portfolioId, this.assetHoldingId);
    }
    
}
