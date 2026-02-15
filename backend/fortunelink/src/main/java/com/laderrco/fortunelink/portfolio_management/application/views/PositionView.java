package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

public record PositionView(
        AssetSymbol symbol,
        AssetType type,
        Quantity quantity,
        Price totalCostBasis,
        Price averageCostPerUnit,
        Price currentPrice,
        Price marketValue,
        Price unrealizedPnL,
        PercentageChange returnPercentage,
        Instant acquiredDate,
        Instant lastUpdated) {
    public PositionView {

        Objects.requireNonNull(symbol);
        Objects.requireNonNull(type);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(totalCostBasis);
        Objects.requireNonNull(averageCostPerUnit);
        Objects.requireNonNull(currentPrice);
        Objects.requireNonNull(marketValue);
        Objects.requireNonNull(unrealizedPnL);
        Objects.requireNonNull(returnPercentage);
        Objects.requireNonNull(acquiredDate);
        Objects.requireNonNull(lastUpdated);
    }
}