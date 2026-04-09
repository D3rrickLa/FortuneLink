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
import org.springframework.scheduling.annotation.Scheduled;
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

    final int batchSize = symbols.size();

    // --- ATOMIC QUOTA CHECK & RESERVE ---
    // We attempt to "reserve" the entire batch size upfront.
    int currentCount;
    do {
      currentCount = fmpDailyCallCount.get();
      if (currentCount + batchSize > fmpDailyLimit) {
        log.warn("FMP daily quota reached (Limit: {}). Skipping batch.", fmpDailyLimit);
        return Map.of();
      }
      // Only proceed if we can atomically swap the old value for (old + batchSize)
    } while (!fmpDailyCallCount.compareAndSet(currentCount, currentCount + batchSize));

    Map<AssetSymbol, MarketAssetQuote> results = new HashMap<>();

    for (AssetSymbol symbol : symbols) {
      try {
        // Note: fmpDailyCallCount is already incremented via the CAS loop above
        FmpQuoteResponse raw = fmpClient.getQuote(symbol.symbol());
        if (raw == null)
          continue;

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
    // Single increments are easier: incrementAndGet returns the NEW value
    if (fmpDailyCallCount.incrementAndGet() > fmpDailyLimit) {
      log.warn("FMP daily quota reached. Cannot fetch Asset Info for {}", symbol.symbol());
      // Optional: decrement if you want to be "precise" about failed attempts not counting,
      // but usually, it's safer to leave it to avoid infinite retries.
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

  @Scheduled(cron = "0 0 0 * * *")
  public void resetDailyQuota() {
    fmpDailyCallCount.set(0);
  }
}