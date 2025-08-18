package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public interface MarketDataService {
    public Money calculateHoldingValue(AssetHolding holding, Instant valuationDate);
}
