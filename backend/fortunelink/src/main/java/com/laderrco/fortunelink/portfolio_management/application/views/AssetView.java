package com.laderrco.fortunelink.portfolio_management.application.views;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.Builder;

@Builder
public record AssetView(
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
        Instant lastUpdated) {
    public AssetView {
        
        Objects.requireNonNull(assetId);
        Objects.requireNonNull(symbol);
        Objects.requireNonNull(type);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(costBasis);
        Objects.requireNonNull(averageCostPerUnit);
        Objects.requireNonNull(currentPrice);
        Objects.requireNonNull(currentValue);
        Objects.requireNonNull(unrealizedGain);
        Objects.requireNonNull(unrealizedGainPercentage);
        Objects.requireNonNull(acquiredDate);
        Objects.requireNonNull(lastUpdated);
    }
}