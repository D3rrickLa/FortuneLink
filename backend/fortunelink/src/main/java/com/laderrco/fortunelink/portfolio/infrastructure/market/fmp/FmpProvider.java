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

    int count = symbols.size();
    if (tryReserve(count)) {
      return Map.of();
    }

    Map<AssetSymbol, MarketAssetQuote> results = new HashMap<>();
    for (AssetSymbol symbol : symbols) {
      try {
        FmpQuoteResponse raw = fmpClient.getQuote(symbol.symbol());
        if (raw == null) {
          undoReserve(1); // Refund if provider returns nothing
          continue;
        }

        Currency currency = knownCurrencies.getOrDefault(symbol, Currency.USD);
        MarketAssetQuote quote = responseMapper.toQuote(raw, currency);
        if (quote != null) {
          results.put(symbol, quote);
        } else {
          undoReserve(1); // Refund if mapping fails
        }
      } catch (Exception e) {
        log.warn("Failed to fetch quote for symbol={}: {}", symbol.symbol(), e.getMessage());
        undoReserve(1); // Refund on network/API error
      }
    }
    return Collections.unmodifiableMap(results);
  }

  @Override
  public Optional<MarketAssetInfo> fetchAssetInfo(AssetSymbol symbol) {
    if (tryReserve(1)) {
      return Optional.empty();
    }

    try {
      FmpProfileResponse response = fmpClient.getProfile(symbol.symbol());
      if (response == null) {
        undoReserve(1);
        return Optional.empty();
      }
      return Optional.ofNullable(responseMapper.toAssetInfo(response));
    } catch (Exception e) {
      log.warn("FMP failed to fetch asset info for {}: {}", symbol.symbol(), e.getMessage());
      undoReserve(1); // Refund on failure
      return Optional.empty();
    }
  }

  @Override
  public Map<AssetSymbol, MarketAssetInfo> fetchBatchAssetInfo(Set<AssetSymbol> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      return Collections.emptyMap();
    }

    // Note: Logic here depends on how fmpClient.getBatchProfiles is implemented.
    // Assuming it is one bulk API call:
    if (tryReserve(1)) {
      return Collections.emptyMap();
    }

    try {
      List<String> tickerStrings = symbols.stream().map(AssetSymbol::symbol).toList();
      return fmpClient.getBatchProfiles(tickerStrings).stream().map(responseMapper::toAssetInfo)
          .filter(Objects::nonNull)
          .collect(Collectors.toMap(MarketAssetInfo::symbol, info -> info, (ex, rep) -> ex));
    } catch (Exception e) {
      log.error("Batch asset info failed", e);
      undoReserve(1);
      return Collections.emptyMap();
    }
  }

  @Override
  public List<SymbolSearchResult> searchSymbols(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }

    // Search also consumes 1 credit usually
    if (tryReserve(1)) {
      return List.of();
    }

    try {
      return fmpClient.getSearch(query).stream().map(responseMapper::toSearchResult).toList();
    } catch (Exception e) {
      log.error("FMP symbol search failed for query='{}': {}", query, e.getMessage());
      undoReserve(1);
      return List.of();
    }
  }

  @Override
  public Currency fetchTradingCurrency(AssetSymbol symbol) {
    return fetchAssetInfo(symbol).map(MarketAssetInfo::tradingCurrency).orElse(Currency.of("USD"));
  }

  @Override
  public boolean supportsSymbol(AssetSymbol symbol) {
    return symbol != null && symbol.symbol() != null && symbol.symbol().matches("[A-Z0-9\\.\\-^]+");
  }

  @Override
  public String getProviderName() {
    return "FMP";
  }

  /**
   * Attempts to reserve quota from Redis.
   */
  private boolean tryReserve(int count) {
    String key = "quota:fmp:" + LocalDate.now(ZoneOffset.UTC);
    Long current = redisTemplate.opsForValue().increment(key, count);

    if (current == null) {
      return true;
    }

    if (current <= count) {
      redisTemplate.expire(key, Duration.ofHours(25));
    }

    if (current > fmpDailyLimit) {
      log.warn("FMP daily quota reached. Current: {}, Limit: {}", current, fmpDailyLimit);
      undoReserve(count); // Immediately refund the overage attempt
      return true;
    }
    return false;
  }

  /**
   * Refunds unused quota back to Redis.
   */
  private void undoReserve(int count) {
    try {
      String key = "quota:fmp:" + LocalDate.now(ZoneOffset.UTC);
      redisTemplate.opsForValue().decrement(key, count);
    } catch (Exception e) {
      log.error("Failed to refund FMP quota to Redis", e);
    }
  }

  @Override
  public Optional<MarketAssetQuote> fetchHistoricalQuote(AssetSymbol symbol, Instant date) {
    log.warn("Historical quotes not implemented for FMP (Free Tier limitation)");
    return Optional.empty();
  }
}