package com.laderrco.fortunelink.portfolio.infrastructure.market;

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
 * Our Api will impelment this (i.e. FMP, Alpha Vantage, etc.)
 */
public interface MarketDataProvider {
  Map<AssetSymbol, MarketAssetQuote> fetchBatchQuotes(Set<AssetSymbol> symbols);

  Optional<MarketAssetQuote> fetchHistoricalQuote(AssetSymbol symbol, Instant date);

  Optional<MarketAssetInfo> fetchAssetInfo(AssetSymbol symbol);

  Map<AssetSymbol, MarketAssetInfo> fetchBatchAssetInfo(Set<AssetSymbol> symbols);

  // Returns a list of matches for a search string (e.g., "Apple")
  List<MarketAssetInfo> searchSymbols(String query);

  Currency fetchTradingCurrency(AssetSymbol symbol);

  /**
   * Does this Data Provider Supports this symbol
   * 
   * @param symbol
   * @return
   */
  boolean supports(AssetSymbol symbol);

  /**
   * Get provider name for logging/debugging.
   */
  String getProviderName();

  /**
   * Check if this provider supports the symbol.
   *
   * @param symbol Raw symbol
   * @return true if supported
   */
  boolean supportsSymbol(AssetSymbol symbol);
}
