package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class HoldingIncreasedEvent implements DomainEvent {

    public HoldingIncreasedEvent(PortfolioId portfolioId, AssetHoldingId assetHoldingId, BigDecimal quantity,
            Money pricePerUnit) {
        //TODO Auto-generated constructor stub
    }

    @Override
    public Instant occuredAt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'occuredAt'");
    }

}
