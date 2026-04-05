package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.market.MarketDataProvider;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FmpProvider implements MarketDataProvider {

  private final FmpClient fmpClient;
  private final FmpResponseMapper responseMapper;

  @Override
  // @param knownCurrencies - currencies from info repo
  public Map<AssetSymbol, MarketAssetQuote> fetchBatchQuotes(Set<AssetSymbol> symbols,
      Map<AssetSymbol, Currency> knownCurrencies) { 
    if (symbols == null || symbols.isEmpty())
      return Map.of();

    Map<AssetSymbol, MarketAssetQuote> results = new HashMap<>();

    for (AssetSymbol symbol : symbols) {
      try {
        FmpQuoteResponse raw = fmpClient.getQuote(symbol.symbol());
        if (raw == null)
          continue;

        // Use stored currency, fall back to USD with a warning
        Currency currency = knownCurrencies.getOrDefault(symbol, null);
        if (currency == null) {
          log.warn("No stored trading currency for {}. Defaulting to USD. " +
              "Record a transaction first to seed asset info.", symbol.symbol());
          currency = Currency.USD;
        }

        MarketAssetQuote quote = responseMapper.toQuote(raw, currency);
        if (quote != null) {
          results.put(symbol, quote);
        }
      } catch (Exception e) {
        // One bad symbol must not kill the entire portfolio load
        log.warn("Failed to fetch quote for symbol={}: {}", symbol.symbol(), e.getMessage());
      }
    }
    return Collections.unmodifiableMap(results);
  }

  @Override
  public Optional<MarketAssetQuote> fetchHistoricalQuote(AssetSymbol symbol, Instant date) {
    log.warn("Historical quotes not implemented for FMP (Free Tier limitation)");
    return Optional.empty();
  }

  @Override
  public Optional<MarketAssetInfo> fetchAssetInfo(AssetSymbol symbol) {
    try {
      FmpProfileResponse response = fmpClient.getProfile(symbol.symbol());
      return Optional.ofNullable(responseMapper.toAssetInfo(response));
    } catch (Exception e) {
      log.warn("FMP failed to fetch asset info for {}: {}", symbol.symbol(), e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Map<AssetSymbol, MarketAssetInfo> fetchBatchAssetInfo(Set<AssetSymbol> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      return Collections.emptyMap();
    }

    List<String> tickerStrings = symbols.stream()
        .map(AssetSymbol::symbol)
        .toList();

    // Note: Ensure your FmpClient has a getBatchProfiles method that iterates
    return fmpClient.getBatchProfiles(tickerStrings).stream()
        .map(responseMapper::toAssetInfo)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            MarketAssetInfo::symbol,
            info -> info,
            (existing, replacement) -> existing));
  }

  @Override
  public List<SymbolSearchResult> searchSymbols(String query) {
    if (query == null || query.isBlank())
      return List.of();

    try {
      return fmpClient.getSearch(query).stream()
          .map(responseMapper::toSearchResult)
          .filter(Objects::nonNull)
          .toList();
    } catch (Exception e) {
      log.error("FMP symbol search failed for query='{}': {}", query, e.getMessage());
      return List.of(); // search failure is non-fatal, return empty list
    }
  }

  @Override
  public Currency fetchTradingCurrency(AssetSymbol symbol) {
    // Defensive check: Try to get currency from profile, fallback to USD
    return fetchAssetInfo(symbol)
        .map(MarketAssetInfo::tradingCurrency)
        .orElse(Currency.of("USD"));
  }

  @Override
  public boolean supportsSymbol(AssetSymbol symbol) {
    if (symbol == null || symbol.symbol() == null)
      return false;
    // Matches standard tickers, tickers with dots (BRK.B), or hyphens
    // (indices/crypto)
    return symbol.symbol().matches("[A-Z0-9\\.\\-^]+");
  }

  @Override
  public String getProviderName() {
    return "FMP";
  }
}