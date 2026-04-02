package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;


import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

/**
 * @param symbol          The unique ticker symbol of the asset (e.g., AAPL).
 * @param name            The full legal name of the entity (e.g., "Apple Inc.").
 * @param type            The category of the asset (e.g., STOCK, ETF, CRYPTO).
 * @param exchange        The primary exchange where the asset is traded (e.g., "NASDAQ").
 * @param tradingCurrency The ISO currency code used for pricing and settlement.
 * @param sector          The industry sector the asset belongs to (e.g., "Technology").
 * @param description     A concise summary or profile of the asset.
 * @implNote: We need an DB, this barely change so we should store it and not waste tokens
 * Represents metadata for a tradable asset within the market system. This includes core
 * identifiers, classification, and descriptive information used for market analysis and portfolio
 * tracking.
 */
public record MarketAssetInfo(
    AssetSymbol symbol,
    String name,
    AssetType type,
    String exchange,
    Currency tradingCurrency,
    String sector,
    String description) {
}