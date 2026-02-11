package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

public interface MarketDataService {
    /**
     * Get current market quote for a single asset.
     * 
     * Returns full quote object with price, volume, timestamp, etc.
     * Use this when you need more than just price (e.g., for charts).
     * 
     * @param symbol The asset symbol (e.g., "AAPL", "BTC-USD")
     * @return Quote if found, empty if symbol doesn't exist
     */
    Optional<MarketAssetQuote> getCurrentQuote(AssetSymbol symbol);

    /**
     * Convenience method - extracts just the current price.
     * 
     * Use this when you only need price for calculations.
     * 
     * @param symbol The asset symbol (e.g., "AAPL", "BTC-USD")
     * @return price of asset found
     * @throws MarketDataException if price unavailable (fail-fast for calculations).
     */
    default Price getCurrentPrice(AssetSymbol symbol) {
        return getCurrentQuote(symbol)
                .map(MarketAssetQuote::currentPrice)
                .orElseThrow(() -> new MarketDataException("Price unavailable for: " + symbol.value()));
    }

    /**
     * Batch fetch current quotes for multiple assets.
     * 
     * More efficient than calling getCurrentQuote() in a loop.
     * Use this when updating entire portfolio valuations.
     * 
     * @param symbols List of symbols to fetch
     * @return Map of symbol → quote (missing symbols not included in map)
     */
    Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(List<AssetSymbol> symbols);

    /**
     * Get historical quote for an asset at a specific point in time.
     * 
     * Use for:
     * - Performance calculations (ROI since purchase)
     * - Historical portfolio valuations
     * - Tax reporting (cost basis at specific date)
     * 
     * @param symbol The asset symbol
     * @param date   The historical date/time
     * @return Quote if available, empty if data doesn't exist
     */
    Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date);

    /**
     * Get descriptive information about an asset.
     * 
     * Use for:
     * - Validating symbol exists before allowing purchase
     * - Displaying asset name instead of just ticker
     * - Auto-detecting asset type (stock vs ETF vs crypto)
     * - Enriching transaction history display
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
     * @param symbols List of symbols
     * @return Map of symbol → info (missing symbols not included)
     */
    Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(List<AssetSymbol> symbols);

    /**
     * Get the currency in which this asset trades.
     * 
     * Examples:
     * - "AAPL" → USD (trades on NASDAQ in USD)
     * - "SHOP.TO" → CAD (trades on TSX in CAD)
     * - "BTC-USD" → USD
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
