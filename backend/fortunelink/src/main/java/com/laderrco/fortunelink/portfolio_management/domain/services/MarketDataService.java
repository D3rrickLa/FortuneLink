package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public interface MarketDataService {
    public Money getCurrentPrice(AssetIdentifier assetIdentifierId);
    public Money getHistoricalPrice(AssetIdentifier assetIdentifierId, LocalDateTime time);
    public Map<AssetIdentifier, Money> getBatchPrices(List<AssetIdentifier> assetIdentifierIds);
}
