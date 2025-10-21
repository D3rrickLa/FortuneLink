package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;

public interface MarketDataService {
    public Price getCurrentPrice(AssetIdentifier assetIdentifier);
}
