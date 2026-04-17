package com.laderrco.fortunelink.portfolio.infrastructure.market;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.redis.CacheKeyFactory;
import com.laderrco.fortunelink.portfolio.infrastructure.exceptions.UnknownSymbolException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/*
User types "App" in search box
    → searchSymbols("App")
    → FMP /search endpoint
    → List<SymbolSearchResult>   ← shallow, UI only

User selects AAPL, records a BUY
    → TransactionService.recordPurchase()
    → validateAndGet(AAPL)       ← confirms symbol is real, seeds info cache
    → FMP /profile/AAPL
    → MarketAssetInfo saved to market_asset_info table

User opens portfolio page
    → getBatchQuotes({AAPL, ...})
    → Redis hit? return cached quote
    → Redis miss? look up currency from market_asset_info, then fetch from FMP
    → Currency is always from stored profile data, never guessed
*/

@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {
  private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);
  private final MarketDataProvider provider;
  private final MarketAssetInfoRepository infoRepository;
  private final RedisTemplate<String, MarketAssetQuote> quoteRedis;
  private final RedisTemplate<String, MarketAssetInfo> infoRedis;
  private final CacheKeyFactory keyFactory;

  @Value("${fortunelink.cache.ttl.current-prices}")
  private long quoteTtl;

  @Value("${fortunelink.cache.ttl.asset-info}")
  private long assetInfoTtl;

  @Value("${fortunelink.cache.ttl.historical-prices}")
  private long historicalTtl;

  @Value("${fortunelink.cache.ttl.trading-currency}")
  private long currencyTtl;

  @Override
  public Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols) {
    if (symbols.isEmpty()) {
      return Map.of();
    }

    List<AssetSymbol> symbolList = new ArrayList<>(symbols);
    List<String> keys = symbolList.stream().map(s -> keyFactory.price(s.symbol())).toList();

    List<MarketAssetQuote> cachedList = quoteRedis.opsForValue().multiGet(keys);

    Map<AssetSymbol, MarketAssetQuote> result = new HashMap<>();
    Set<AssetSymbol> misses = new HashSet<>();

    for (int i = 0; i < symbolList.size(); i++) {
      MarketAssetQuote cached = (cachedList != null) ? cachedList.get(i) : null;
      if (cached != null) {
        result.put(symbolList.get(i), cached);
      } else {
        misses.add(symbolList.get(i));
      }
    }

    if (!misses.isEmpty()) {
      Map<AssetSymbol, Currency> currencies = infoRepository.findBySymbols(misses).entrySet()
          .stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().tradingCurrency()));

      Map<AssetSymbol, MarketAssetQuote> fetched = provider.fetchBatchQuotes(misses, currencies);
      result.putAll(fetched);

      // Cache results
      Map<String, MarketAssetQuote> toCache = new HashMap<>();
      fetched.forEach((sym, quote) -> toCache.put(keyFactory.price(sym.symbol()), quote));

      writeQuotesToCache(toCache);
    }

    return result;
  }

  @Override
  public Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols) {
    if (symbols.isEmpty()) {
      return Map.of();
    }

    List<AssetSymbol> symbolList = new ArrayList<>(symbols);
    List<String> keys = symbolList.stream().map(s -> keyFactory.assetInfo(s.symbol())).toList();

    List<MarketAssetInfo> cachedList = infoRedis.opsForValue().multiGet(keys);

    Map<AssetSymbol, MarketAssetInfo> result = new HashMap<>();
    Set<AssetSymbol> misses = new HashSet<>();

    for (int i = 0; i < symbolList.size(); i++) {
      MarketAssetInfo cached = (cachedList != null) ? cachedList.get(i) : null;
      if (cached != null) {
        result.put(symbolList.get(i), cached);
      } else {
        misses.add(symbolList.get(i));
      }
    }

    // DB fallback
    if (!misses.isEmpty()) {
      Map<AssetSymbol, MarketAssetInfo> dbResults = infoRepository.findBySymbols(misses);

      result.putAll(dbResults);
      misses.removeAll(dbResults.keySet());
    }

    // Provider fallback
    if (!misses.isEmpty()) {
      Map<AssetSymbol, MarketAssetInfo> fetched = provider.fetchBatchAssetInfo(misses);

      result.putAll(fetched);

      if (!fetched.isEmpty()) {
        try {
          infoRepository.saveAll(fetched);
        } catch (Exception e) {
          log.warn("Failed to persist asset info: {}", e.getMessage());
        }
      }
    }

    // Cache everything we now have
    Map<String, MarketAssetInfo> toCache = new HashMap<>();
    result.forEach((sym, info) -> toCache.put(keyFactory.assetInfo(sym.symbol()), info));

    writeAssetInfoToCache(toCache);

    return result;
  }

  @Override
  public Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol) {
    Map<AssetSymbol, MarketAssetInfo> result = getBatchAssetInfo(Collections.singleton(symbol));

    return Optional.ofNullable(result.get(symbol));
  }

  @Override
  public Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date) {
    String key = keyFactory.historical(symbol.symbol(), date);

    MarketAssetQuote cached = quoteRedis.opsForValue().get(key);
    if (cached != null) {
      return Optional.of(cached);
    }

    Optional<MarketAssetQuote> fetched = provider.fetchHistoricalQuote(symbol, date);

    fetched.ifPresent(q -> {
      quoteRedis.opsForValue().set(key, q, Duration.ofSeconds(historicalTtl));
    });

    return fetched;
  }

  @Override
  public Currency getTradingCurrency(AssetSymbol symbol) {
    // Fallback safety (should rarely happen)
    return getAssetInfo(symbol).map(MarketAssetInfo::tradingCurrency).orElseGet(() -> {
      Currency fetched = provider.fetchTradingCurrency(symbol);
      log.warn("Currency fallback used for {}", symbol.symbol());
      return fetched;
    });
  }

  @Override
  public List<SymbolSearchResult> searchSymbols(String query) {
    return provider.searchSymbols(query);
  }

  @Override
  public boolean isSymbolSupported(AssetSymbol symbol) {
    return provider.supportsSymbol(symbol);
  }

  @Override
  public MarketAssetInfo validateAndGet(AssetSymbol symbol) {
    return getAssetInfo(symbol).orElseThrow(() -> new UnknownSymbolException(symbol.symbol()));
  }

  private void writeAssetInfoToCache(Map<String, MarketAssetInfo> data) {
    if (data.isEmpty()) {
      return;
    }

    infoRedis.opsForValue().multiSet(data);
    data.keySet().forEach(k -> infoRedis.expire(k, Duration.ofSeconds(assetInfoTtl)));
  }

  private void writeQuotesToCache(Map<String, MarketAssetQuote> data) {
    if (data.isEmpty()) {
      return;
    }

    quoteRedis.opsForValue().multiSet(data);
    data.keySet().forEach(k -> quoteRedis.expire(k, Duration.ofSeconds(quoteTtl)));
  }
}