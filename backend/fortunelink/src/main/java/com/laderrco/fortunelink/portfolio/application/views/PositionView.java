package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;

import java.time.Instant;

/**
 * View representation of a position.
 * * DESIGN REALITY:
 * totalCostBasis = (Quantity * Price) + Purchase Fees.
 * This aligns with CRA Section 53 (Adjusted Cost Base).
 */
public record PositionView(
		String symbol,
		AssetType assetType,
		Quantity quantity,

		// The true ACB (includes purchase commissions)
		Price totalCostBasis,

		// totalCostBasis / quantity (includes the "fee per unit")
		Price averageCostPerUnit,

		// FOR DISPLAY ONLY: Represents the portion of totalCostBasis attributed to
		// fees.
		// Logic: Do NOT add this to totalCostBasis; it is already inside.
		Price totalFeesIncurred,

		Price currentPrice,
		Price marketValue,

		// marketValue - totalCostBasis.
		// This is the "Net" P&L after accounting for the cost of buying.
		Price unrealizedPnL,

		PercentageChange returnPercentage,
		String costBasisMethod,
		Instant firstAcquired,
		Instant lastModified) {
}