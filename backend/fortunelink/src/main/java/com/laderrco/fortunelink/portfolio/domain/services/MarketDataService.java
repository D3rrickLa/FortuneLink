package com.laderrco.fortunelink.portfolio.domain.services;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public interface MarketDataService {
    /**
     * Batch fetch current quotes for multiple assets.
     * Also acts as our 'single get'
     * 
     * More efficient than calling getCurrentQuote() in a loop. Use this when
     * updating entire
     * portfolio valuations.
     * 
     * @param symbols List of symbols to fetch
     * @return Map of symbol → quote (missing symbols not included in map)
     */
    Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols);

    /**
     * Get historical quote for an asset at a specific point in time.
     * 
     * Use for: - Performance calculations (ROI since purchase) - Historical
     * portfolio valuations -
     * Tax reporting (cost basis at specific date)
     * 
     * @param symbol The asset symbol
     * @param date   The historical date/time
     * @return Quote if available, empty if data doesn't exist
     */
    Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date);

    /**
     * Get descriptive information about an asset.
     * 
     * Use for: - Validating symbol exists before allowing purchase - Displaying
     * asset name instead
     * of just ticker - Auto-detecting asset type (stock vs ETF vs crypto) -
     * Enriching transaction
     * history display
     * 
     * @param symbol The asset symbol
     * @return Asset info if found, empty if symbol doesn't exist
     */
    Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol);

    /**
     * Batch fetch asset metadata for multiple symbols.
     * 
     * Use when building portfolio summary view.
     * 
     * @param symbols set of symbols
     * @return Map of symbol → info (missing symbols not included)
     */
    Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols);

    /**
     * Get the currency in which this asset trades.
     * 
     * Examples: - "AAPL" → USD (trades on NASDAQ in USD) - "SHOP.TO" → CAD (trades
     * on TSX in CAD) -
     * "BTC-USD" → USD
     * 
     * Use for currency conversion in transactions.
     * 
     * @param symbol The symbol to check
     * @return the currency for said asset
     */
    Currency getTradingCurrency(AssetSymbol symbol);

    /**
     * Check if a symbol is supported by the data provider.
     * 
     * Use for validation before allowing user to add asset.
     * 
     * @param symbol The symbol to check
     * @return true if data available, false otherwise
     */
    boolean isSymbolSupported(AssetSymbol symbol);

}
