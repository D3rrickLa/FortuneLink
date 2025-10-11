package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;

public record HoldingDecreasedEvent(PortfolioId portfolioId, AssetHoldingId assetHoldingId, Quantity quantity, Price pricePerUnit, Money realizedGainLoss, Instant transactionDate) implements DomainEvent {

    @Override
    public Instant occuredOn() {
        return this.transactionDate;
    }

    @Override
    public String eventType() {
        return "Decrease position event."; // probably want an enum for this
    }

    @Override
    public String aggregateId() {
        return String.format("{PortfolioId: %s, AssetHoldingId: %s}", this.portfolioId, this.assetHoldingId);
    }

}
