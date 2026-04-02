package com.laderrco.fortunelink.portfolio.infrastructure.market;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {
  private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);

  private final MarketDataProvider provider;
  private final MarketAssetInfoRepository infoRepository;
  private final RedisTemplate<String, MarketAssetQuote> redisTemplate;

  @Value("${fortunelink.cache.key-prefix.prices}")
  private String cacheKeyPrefix;

  @Value("${fortunelink.cache.ttl.current-prices}")
  private long quoteTtl;

  /**
   * Manual caching for batch quotes to ensure MGET/MSET efficiency.
   */
  @Override
  public Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols) {
    if (symbols.isEmpty())
      return Map.of();

    List<String> keys = symbols.stream()
        .map(s -> cacheKeyPrefix + s.symbol())
        .toList();

    List<MarketAssetQuote> values = redisTemplate.opsForValue().multiGet(keys);

    Map<AssetSymbol, MarketAssetQuote> result = new HashMap<>();
    Set<AssetSymbol> misses = new HashSet<>();

    List<AssetSymbol> symbolList = new ArrayList<>(symbols);
    for (int i = 0; i < symbolList.size(); i++) {
      if (values != null && values.get(i) != null) {
        result.put(symbolList.get(i), values.get(i));
      } else {
        misses.add(symbolList.get(i));
      }
    }

    if (!misses.isEmpty()) {
      Map<AssetSymbol, MarketAssetQuote> fetched = provider.fetchBatchQuotes(misses);
      Map<String, MarketAssetQuote> toCache = new HashMap<>();
      fetched.forEach((sym, quote) -> {
        result.put(sym, quote);
        toCache.put(cacheKeyPrefix + sym.symbol(), quote);
      });

      if (!toCache.isEmpty()) {
        redisTemplate.opsForValue().multiSet(toCache);
        // Redis doesn't support MSET + EXPIRE in one go; looping is necessary
        toCache.keySet().forEach(k -> redisTemplate.expire(k, Duration.ofSeconds(quoteTtl)));
      }
    }
    return result;
  }

  @Override
  @Cacheable(value = "${fortunelink.cache.key-prefix.historical}", key = "#symbol.symbol()", unless = "#result == null")
  public Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date) {
    return provider.fetchHistoricalQuote(symbol, date);
  }

  @Override
  @Cacheable(value = "${fortunelink.cache.key-prefix.asset-info}", key = "#symbol.symbol()", unless = "#result == null")
  public Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol) {
    // Checking DB directly is fine, but Spring Cache will wrap this whole method.
    // If it's in Redis, this code won't even execute.
    Optional<MarketAssetInfo> cached = infoRepository.findBySymbol(symbol);
    if (cached.isPresent()) {
      return cached;
    }

    Optional<MarketAssetInfo> fetched = provider.fetchAssetInfo(symbol);
    fetched.ifPresent(info -> {
      try {
        infoRepository.save(info);
      } catch (Exception e) {
        log.warn("Failed to cache asset info for {}: {}", symbol.symbol(), e.getMessage());
      }
    });

    return fetched;
  }

  @Override
  @Cacheable(value = "${fortunelink.cache.key-prefix.currency}", key = "#symbol.symbol()", unless = "#result == null")
  public Currency getTradingCurrency(AssetSymbol symbol) {
    return provider.fetchTradingCurrency(symbol);
  }

  @Override
  public boolean isSymbolSupported(AssetSymbol symbol) {
    return provider.supportsSymbol(symbol);
  }

  // Note: getBatchAssetInfo uses DB directly (infoRepository),
  // so it doesn't need @Cacheable (Redis) unless you want a two-tier cache.
  @Override
  public Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols) {
    if (symbols.isEmpty())
      return Map.of();

    Map<AssetSymbol, MarketAssetInfo> result = new HashMap<>(infoRepository.findBySymbols(symbols));
    Set<AssetSymbol> misses = new HashSet<>(symbols);
    misses.removeAll(result.keySet());

    if (!misses.isEmpty()) {
      Map<AssetSymbol, MarketAssetInfo> fetched = provider.fetchBatchAssetInfo(misses);
      result.putAll(fetched);
      if (!fetched.isEmpty()) {
        try {
          infoRepository.saveAll(fetched);
        } catch (Exception e) {
          log.warn("Failed to batch-persist asset info: {}", e.getMessage());
        }
      }
    }
    return result;
  }
}