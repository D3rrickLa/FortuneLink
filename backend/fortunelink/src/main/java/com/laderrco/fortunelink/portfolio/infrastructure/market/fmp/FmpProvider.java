package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.market.MarketDataProvider;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class FmpProvider implements MarketDataProvider {
  private final FmpClient fmpClient;
  private final FmpResponseMapper responseMapper;
  private final StringRedisTemplate redisTemplate;
  @Value("${fortunelink.rate-limit.fmp-quota.daily-limit:250}")
  private int fmpDailyLimit;

  @Override
  public Map<AssetSymbol, MarketAssetQuote> fetchBatchQuotes(Set<AssetSymbol> symbols,
      Map<AssetSymbol, Currency> knownCurrencies) {

    if (symbols == null || symbols.isEmpty()) {
      return Map.of();
    }

    if (!tryReserve(symbols.size())) {
      return Map.of();
    }

    Map<AssetSymbol, MarketAssetQuote> results = new HashMap<>();
    for (AssetSymbol symbol : symbols) {
      try {
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
    if (!tryReserve(1)) {
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

  private boolean tryReserve(int count) {
    String key = "quota:fmp:" + LocalDate.now(ZoneOffset.UTC);

    // Atomically increment
    Long current = redisTemplate.opsForValue().increment(key, count);

    if (current == null) {
      return false;
    }

    // If this is the first call of the day, set expiration so Redis cleans up
    if (current <= count) {
      redisTemplate.expire(key, Duration.ofHours(25));
    }

    if (current > fmpDailyLimit) {
      log.warn("FMP daily quota reached. Current: {}, Limit: {}", current, fmpDailyLimit);
      return false;
    }
    return true;
  }
}