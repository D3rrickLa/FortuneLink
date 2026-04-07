package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.market.MarketDataProvider;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class FmpProvider implements MarketDataProvider {
  private final FmpClient fmpClient;
  private final FmpResponseMapper responseMapper;
  // AtomicInteger is thread-safe for multi-user access
  private final AtomicInteger fmpDailyCallCount = new AtomicInteger(0);
  @Value("${fortunelink.rate-limit.fmp-quota.daily-limit:250}")
  private int fmpDailyLimit;

  @Override
  public Map<AssetSymbol, MarketAssetQuote> fetchBatchQuotes(Set<AssetSymbol> symbols,
      Map<AssetSymbol, Currency> knownCurrencies) {
    if (symbols == null || symbols.isEmpty()) {
      return Map.of();
    }

    // --- QUOTA CHECK ---
    // We check if the NEXT batch of calls will put us over.
    // FMP Free Tier doesn't have a true 'batch' quote endpoint for all symbols,
    // so each loop iteration is a billing hit.
    if (fmpDailyCallCount.get() + symbols.size() > fmpDailyLimit) {
      log.warn(
          "FMP daily quota reached (Limit: {}). Skipping API calls to prevent 429/Account suspension.",
          fmpDailyLimit);
      return Map.of(); // Return empty to trigger fallback to cost basis in ValuationService
    }

    Map<AssetSymbol, MarketAssetQuote> results = new HashMap<>();

    for (AssetSymbol symbol : symbols) {
      try {
        // Increment counter for every actual network call
        fmpDailyCallCount.incrementAndGet();

        FmpQuoteResponse raw = fmpClient.getQuote(symbol.symbol());
        if (raw == null) {
          continue;
        }

        Currency currency = knownCurrencies.getOrDefault(symbol, Currency.USD);
        MarketAssetQuote quote = responseMapper.toQuote(raw, currency);
        if (quote != null) {
          results.put(symbol, quote);
        }
      } catch (Exception e) {
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
    if (fmpDailyCallCount.incrementAndGet() > fmpDailyLimit) {
      log.warn("FMP daily quota reached. Cannot fetch Asset Info for {}", symbol.symbol());
      return Optional.empty();
    }

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

    List<String> tickerStrings = symbols.stream().map(AssetSymbol::symbol).toList();

    // Note: Ensure your FmpClient has a getBatchProfiles method that iterates
    return fmpClient.getBatchProfiles(tickerStrings).stream().map(responseMapper::toAssetInfo)
        .filter(Objects::nonNull).collect(Collectors.toMap(MarketAssetInfo::symbol, info -> info,
            (existing, replacement) -> existing));
  }

  @Override
  public List<SymbolSearchResult> searchSymbols(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }

    try {
      return fmpClient.getSearch(query).stream().map(responseMapper::toSearchResult)
          .filter(Objects::nonNull).toList();
    } catch (Exception e) {
      log.error("FMP symbol search failed for query='{}': {}", query, e.getMessage());
      return List.of(); // search failure is non-fatal, return empty list
    }
  }

  @Override
  public Currency fetchTradingCurrency(AssetSymbol symbol) {
    // Defensive check: Try to get currency from profile, fallback to USD
    return fetchAssetInfo(symbol).map(MarketAssetInfo::tradingCurrency).orElse(Currency.of("USD"));
  }

  @Override
  public boolean supportsSymbol(AssetSymbol symbol) {
    if (symbol == null || symbol.symbol() == null) {
      return false;
    }
    // Matches standard tickers, tickers with dots (BRK.B), or hyphens
    // (indices/crypto)
    return symbol.symbol().matches("[A-Z0-9\\.\\-^]+");
  }

  @Override
  public String getProviderName() {
    return "FMP";
  }

  // This method resets the counter at midnight (or whenever the app restarts)
  // For a production app, you'd likely use a Redis key that expires at midnight
  // to track this across app restarts, but AtomicInteger works for a single
  // instance.
  public void resetDailyQuota() {
    fmpDailyCallCount.set(0);
  }
}