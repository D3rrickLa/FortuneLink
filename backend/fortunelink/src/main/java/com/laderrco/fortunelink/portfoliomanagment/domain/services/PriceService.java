package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

// for fetching current market price of a stock/other asset
public interface PriceService {

    public Money getCurrentAssetPrice(AssetIdentifier assetIdentifier, Instant time);
    public Map<AssetIdentifier, MarketPrice> getCurrentPrices(Set<AssetIdentifier> assetIdentifiers);
    public Map<AssetIdentifier, MarketPrice> getHistoricalPrices(Set<AssetIdentifier> assetIdentifiers, Instant time);
    public MarketPrice getCurrentPrice(AssetIdentifier assetIdentifier); // Returns MarketPrice, not Money    
}