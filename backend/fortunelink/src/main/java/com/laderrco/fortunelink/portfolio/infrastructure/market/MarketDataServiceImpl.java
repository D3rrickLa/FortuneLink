package com.laderrco.fortunelink.portfolio.infrastructure.market;

import java.time.Instant;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;

import lombok.RequiredArgsConstructor;

// NOTE: Probably rename this to MarketDataServiceImpl.java
@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {
  private static Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);
  private final MarketDataProvider provider;
  private final MarketAssetInfoRepository infoRepository;
  private final CacheManager cacheManager;

  @Value("${fortunelink.cache.key-prefix.prices}")
  private String cacheKeyPrefix;

  @Value("${fortunelink.cache.ttl.current-prices}")
  private long quoteTtl;

  /*
   * Quotes are provided, Redis should hanle them. We don't add DB caching here
   */
  @Override
  public Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols) {
    if (symbols.isEmpty()) {
      return Map.of();
    }

    Cache cache = cacheManager.getCache("current-prices");
    Map<AssetSymbol, MarketAssetQuote> result = new HashMap<>();
    Set<AssetSymbol> misses = new HashSet<>();

    for (AssetSymbol symbol : symbols) {
      if (cache != null) {
        Cache.ValueWrapper wrapper = cache.get(symbol.symbol());
        if (wrapper != null && wrapper.get() instanceof MarketAssetQuote quote) {
          result.put(symbol, quote);
          continue;
        }
      }
      misses.add(symbol);
    }

    if (!misses.isEmpty()) {
      Map<AssetSymbol, MarketAssetQuote> fetched = provider.fetchBatchQuotes(misses);
      fetched.forEach((sym, quote) -> {
        result.put(sym, quote);
        if (cache != null) {
          cache.put(sym.symbol(), quote);
        }
      });
    }

    return result;
  }

  @Override
  @Cacheable(value = "${fortunelink.cache.key-prefix.prices}", key = "#symbol.symbol()", unless = "#result == null")
  public Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date) {
    return provider.fetchHistoricalQuote(symbol, date);
  }

  @Override
  @Cacheable(value = "${fortunelink.cache.key-prefix.asset-info}", key = "#symbol.symbol()", unless = "#result == null")
  public Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol) {
    Optional<MarketAssetInfo> cached = infoRepository.findBySymbol(symbol);
    if (cached.isPresent()) {
      return cached;
    }

    Optional<MarketAssetInfo> fetched = provider.fetchAssetInfo(symbol);
    fetched.ifPresent(info -> {
      try {
        infoRepository.save(info);
      } catch (Exception e) {
        // Non-fatal: we got the data, just couldn't cache it
        log.warn("Failed to cache asset info for {}: {}", symbol.symbol(), e.getMessage());
      }
    });

    return fetched;
  }

  @Override
  public Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols) {
    if (symbols.isEmpty()) {
      return Map.of();
    }

    // DB first, covers all symbols in one query
    Map<AssetSymbol, MarketAssetInfo> result = new HashMap<>(
        infoRepository.findBySymbols(symbols));

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

  @Override
  public Currency getTradingCurrency(AssetSymbol symbol) {
    return provider.fetchTradingCurrency(symbol);
  }

  @Override
  public boolean isSymbolSupported(AssetSymbol symbol) {
    return provider.supportsSymbol(symbol);
  }

}
