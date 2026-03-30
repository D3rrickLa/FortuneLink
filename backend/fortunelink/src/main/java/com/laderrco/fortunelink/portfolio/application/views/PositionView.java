package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import java.time.Instant;

/**
 * View representation of a position. * DESIGN REALITY: totalCostBasis = (Quantity * Price) +
 * Purchase Fees. This aligns with CRA Section 53 (Adjusted Cost Base).
 */
public record PositionView(
    String symbol,
    AssetType assetType,
    Quantity quantity,
    Price totalCostBasis,
    Price averageCostPerUnit,
    Money totalFeesIncurred,
    Price currentPrice,
    Money marketValue,
    Money unrealizedPnL,
    PercentageChange returnPercentage,
    String costBasisMethod,
    Instant firstAcquiredDate,
    Instant lastModified) {
}