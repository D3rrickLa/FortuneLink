package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public record AssetResponse(
    AssetId assetId,
    String symbol,
    AssetType type,
    BigDecimal quantity,
    Money costBasis,
    Money averageCostPerUnit,
    Money currentPrice,
    Money currentValue,
    Money unrealizedGain,
    Percentage unrealizedGainPercentage,
    Instant acquiredDate,
    Instant lastUpdated
) {
}