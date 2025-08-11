package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public record AssetBoughtEvent(
    PortfolioId portfolioId, 
    AssetHoldingId assetHoldingId, 
    AssetIdentifier assetIdentifier,
    BigDecimal quantity, 
    Money totalCost, 
    Instant now
)  {


}
