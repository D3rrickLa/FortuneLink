package com.laderrco.fortunelink.portfolio.infrastructure.market;

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
 * Our Api will impelment this (i.e. FMP, Alpha Vantage, etc.) NOTE: Primary is on FMP, will be
 * swtiching to Yahoo in the future
 */
public interface MarketDataProvider {
  Map<AssetSymbol, MarketAssetQuote> fetchBatchQuotes(Set<AssetSymbol> symbols,
      Map<AssetSymbol, Currency> knownCurrencies);

  Optional<MarketAssetQuote> fetchHistoricalQuote(AssetSymbol symbol, Instant date);

  Optional<MarketAssetInfo> fetchAssetInfo(AssetSymbol symbol);

  Map<AssetSymbol, MarketAssetInfo> fetchBatchAssetInfo(Set<AssetSymbol> symbols);

  /**
   * Symbol autocomplete for UI search boxes. Returns shallow results only , do NOT use for
   * transaction validation. For transaction validation, use validateAndGet(AssetSymbol).
   */
  List<SymbolSearchResult> searchSymbols(String query);

  Currency fetchTradingCurrency(AssetSymbol symbol);

  /**
   * Check if this provider supports the symbol.
   *
   * @param symbol Raw symbol
   * @return true if supported
   */
  boolean supportsSymbol(AssetSymbol symbol);

  String getProviderName();

}
