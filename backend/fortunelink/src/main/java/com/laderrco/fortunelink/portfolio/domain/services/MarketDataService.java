package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Primary gateway for retrieving financial market data.
 * <p>
 * This service provides a unified interface to fetch real-time quotes, historical prices, and asset
 * metadata, shielding the rest of the application from the complexities of specific external market
 * data providers.
 */
public interface MarketDataService {
  /**
   * Returns current quotes for the specified assets. NOTE: we need to confirm the impl of this. If
   * aa portfolio has accounts with nothing in them, extractSymbols returns empty set. but our
   * MarketDataService.java contract says missing symbols are excluded from the result map. We need
   * to verify this impl handles getBatchQuotes(Set.of()) without hitting the API
   *
   * @param symbols a set of asset symbols to fetch
   * @return a map of symbol to quote; symbols not found are excluded
   */
  Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols);

  /**
   * Returns the price of an asset at a specific time.
   *
   * @param symbol the asset symbol
   * @param date   the point in time for the historical quote
   * @return the quote, or empty if no data exists for that time
   */
  Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date);

  /**
   * Returns descriptive details for a single asset.
   *
   * @param symbol the asset symbol
   * @return the asset information, or empty if the symbol is unknown
   */
  Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol);

  /**
   * Returns descriptive details for multiple assets.
   *
   * @param symbols a set of asset symbols
   * @return a map of symbol to information; symbols not found are excluded
   */
  Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols);

  /**
   * Symbol autocomplete for UI search boxes. Returns shallow results only — do NOT use for
   * transaction validation. For transaction validation, use validateAndGet(AssetSymbol).
   */
  List<SymbolSearchResult> searchSymbols(String query);

  /**
   * Validates that a symbol exists and returns its full asset info. Use this at transaction
   * recording time to confirm a symbol is real before persisting a BUY.
   * <p>
   * NOT for search/autocomplete, use searchSymbols(String) for that.
   * <p>
   * Side effect: seeds market_asset_info cache on first call.
   */
  MarketAssetInfo validateAndGet(AssetSymbol symbol);

  /**
   * Returns the currency used for trading the asset.
   *
   * @param symbol the asset symbol
   * @return the currency code, such as USD or CAD
   */
  Currency getTradingCurrency(AssetSymbol symbol);

  /**
   * Validates if the symbol is available via this provider.
   *
   * @param symbol the symbol to check
   * @return true if supported, false otherwise
   */
  boolean isSymbolSupported(AssetSymbol symbol);
}
