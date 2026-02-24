package com.laderrco.fortunelink.portfolio.application.views;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;

/**
 * Read-only view of a single position.
 *
 * Fee display contract (Option A — fees separate from ACB):
 *
 * totalCostBasis Gross cost only (qty × avg price across all buys).
 * This is what the position model stores. Used for
 * unrealized P&L on the portfolio/holdings screen.
 *
 * totalFeesIncurred Cumulative BUY fees paid for this symbol, sourced
 * from Transaction.fees — NOT baked into cost basis.
 * Populated by AccountViewBuilder when a fee map is
 * provided; defaults to Price.ZERO when unavailable
 * (e.g., summary views that don't need tax data).
 *
 * unrealizedPnL marketValue - totalCostBasis (gross, fees excluded).
 *
 * UI rendering guide:
 * Holdings screen: show totalCostBasis, unrealizedPnL, returnPercentage.
 * Optionally show totalFeesIncurred as a secondary line.
 * Tax / ACB screen: effectiveAcb = totalCostBasis + totalFeesIncurred
 * CRA allows adding brokerage fees to ACB — combine at
 * report generation time, not in the domain model.
 *
 * firstAcquired / lastModified are nullable:
 * ACB positions don't track individual lot dates — firstAcquired is null.
 * FIFO derives both from the lot list.
 */
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