package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;

public record HoldingIncreasedEvent(PortfolioId portfolioId, AssetHoldingId assetHoldingId, Quantity quantity, Price pricePerUnit, Instant transactionDate) implements DomainEvent {

    @Override
    public Instant occuredOn() {
        return this.transactionDate;
    }

    @Override
    public String eventType() {
        return "Increase position event."; // probably want an enum for this
    }

    @Override
    public String aggregateId() {
        return String.format("{Portfolio Id: %s, AssetHoldingId: %s}", this.portfolioId, this.assetHoldingId);
    }

}
