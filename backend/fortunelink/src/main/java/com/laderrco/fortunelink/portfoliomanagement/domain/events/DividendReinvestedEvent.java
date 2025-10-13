package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;

public record DividendReinvestedEvent(PortfolioId portfolioId, AssetHoldingId assetHoldingId, Money dividendAmount, Quantity sharesReceived, Price pricePerShare, Instant reinvestmentDate) implements DomainEvent {

    @Override
    public Instant occuredOn() {
        return this.reinvestmentDate;
    }

    @Override
    public String eventType() {
        return "Dividend reinvested event";
    }

    @Override
    public String aggregateId() {
        return String.format("{PortfolioId: %s, AssetHoldingId: %s}", this.portfolioId, this.assetHoldingId);
    }
    
}
