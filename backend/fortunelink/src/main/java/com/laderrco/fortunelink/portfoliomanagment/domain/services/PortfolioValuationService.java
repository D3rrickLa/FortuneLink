package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public interface PortfolioValuationService {
    public Money calculateTotalValue(Portfolio portfolio, ExchangeRate exchangeRate);
    public Money calculateAssetValue(AssetHolding asset, MarketPrice currentPrice);
    public Percentage calculatePortfolioAllocation(Portfolio portfolio, AssetType assetType);
}
