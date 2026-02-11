package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;


import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

public record MarketAssetInfo(
    AssetSymbol symbol,
    String name,              // "Apple Inc."
    AssetType type,           // STOCK
    String exchange,          // "NASDAQ"
    Currency tradingCurrency, // USD
    String sector,            // "Technology"
    String description        // Company description
) {
}