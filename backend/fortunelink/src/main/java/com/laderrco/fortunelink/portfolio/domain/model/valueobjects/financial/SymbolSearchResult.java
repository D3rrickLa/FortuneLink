package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

/**
 * Shallow result from a symbol search query.
 * Intentionally NOT a MarketAssetInfo — it is missing type, sector,
 * and description which require a full profile fetch.
 * Used exclusively for UI autocomplete/search flows.
 */
public record SymbolSearchResult(
    AssetSymbol symbol,
    String name,
    String exchange,
    Currency tradingCurrency) {
}