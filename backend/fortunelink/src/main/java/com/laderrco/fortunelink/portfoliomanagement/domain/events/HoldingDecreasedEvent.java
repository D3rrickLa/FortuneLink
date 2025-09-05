package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class HoldingDecreasedEvent implements DomainEvent {

    public HoldingDecreasedEvent(PortfolioId portfolioId, AssetHoldingId assetHoldingId, BigDecimal quantity,
            Money pricePerUnit, Money realizedGainLoss) {
        //TODO Auto-generated constructor stub
    }

    @Override
    public Instant occuredAt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'occuredAt'");
    }

}
