package com.laderrco.fortunelink.portfoliomanagment.infrastructure.services;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public class SimpleMaketDataService  implements MarketDataService{

    @Override
    public Money calculateHoldingValue(AssetHolding holding, Instant valuationDate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateHoldingValue'");
    }
    
}
