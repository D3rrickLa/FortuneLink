package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;

import java.time.Instant;

public record PositionView(
		String symbol,
		AssetType assetType,
		Quantity quantity,
		Price totalCostBasis, // gross cost, no fees
		Price averageCostPerUnit, // totalCostBasis / quantity
		Price totalFeesIncurred, // cumulative BUY fees — for tax/effective ACB display
		Price currentPrice,
		Price marketValue,
		Price unrealizedPnL, // marketValue - totalCostBasis (gross)
		PercentageChange returnPercentage,
		String costBasisMethod, // "ACB" or "FIFO"
		Instant firstAcquired, // nullable — null for ACB positions
		Instant lastModified // nullable
) {
}