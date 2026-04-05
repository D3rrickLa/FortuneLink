package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
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
  public Map<AssetSymbol, MarketAssetQuote> fetchBatchQuotes(Set<AssetSymbol> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      return Collections.emptyMap();
    }

    List<String> tickerStrings = symbols.stream()
        .map(AssetSymbol::symbol)
        .toList();

    // client.getBatchQuotes performs the sequential mapping for Free Tier
    return fmpClient.getBatchQuotes(tickerStrings).stream()
        .map(responseMapper::toDomain)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            MarketAssetQuote::symbol,
            quote -> quote,
            (existing, replacement) -> existing));
  }

  public Optional<MarketAssetQuote> fetchCurrentQuote(AssetSymbol symbol) {
    try {
      FmpQuoteResponse response = fmpClient.getQuote(symbol.symbol());
      return Optional.ofNullable(responseMapper.toDomain(response));
    } catch (Exception e) {
      log.error("FMP failed to fetch current quote for {}: {}", symbol.symbol(), e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<MarketAssetInfo> fetchAssetInfo(AssetSymbol symbol) {
    try {
      FmpProfileResponse response = fmpClient.getProfile(symbol.symbol());
      return Optional.ofNullable(responseMapper.toDomain(response));
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
        .map(responseMapper::toDomain)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            MarketAssetInfo::symbol,
            info -> info,
            (existing, replacement) -> existing));
  }

  @Override
  public List<MarketAssetInfo> searchSymbols(String query) {
    if (query == null || query.isBlank()) {
      return Collections.emptyList();
    }

    try {
      // Note: Add getSearch(query) to FmpClient returning List<FmpSearchResponse>
      return fmpClient.getSearch(query).stream()
          .map(responseMapper::toDomain)
          .filter(Objects::nonNull)
          .toList();
    } catch (Exception e) {
      log.error("FMP search failed for '{}': {}", query, e.getMessage());
      return Collections.emptyList();
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
  public boolean supports(AssetSymbol symbol) {
    return supportsSymbol(symbol);
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
  public Optional<MarketAssetQuote> fetchHistoricalQuote(AssetSymbol symbol, Instant date) {
    log.warn("Historical quotes not implemented for FMP (Free Tier limitation)");
    return Optional.empty();
  }

  @Override
  public String getProviderName() {
    return "FMP";
  }
}