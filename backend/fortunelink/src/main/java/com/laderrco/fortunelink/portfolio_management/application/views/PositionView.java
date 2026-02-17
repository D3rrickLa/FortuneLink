package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;

public record PositionView(
    String symbol,
    AssetType assetType,
    Quantity quantity,
    Price totalCostBasis,
    Price averageCostPerUnit,
    Price currentPrice,
    Price marketValue,
    Price unrealizedPnL,
    PercentageChange returnPercentage,
    String costBasisMethod,  // "ACB" or "FIFO"
    Instant firstAcquired,   // nullable
    Instant lastModified     // nullable
) {}