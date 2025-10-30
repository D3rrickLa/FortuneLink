package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;

public interface MarketDataService {
    public Price getCurrentPrice(AssetIdentifier assetIdentifier);
    public Price getHistoricalPrice(AssetIdentifier assetIdentifier, LocalDateTime time);
    public Map<AssetIdentifier, Price> getBatchPrices(List<AssetIdentifier> assetIdentifiers);
}
