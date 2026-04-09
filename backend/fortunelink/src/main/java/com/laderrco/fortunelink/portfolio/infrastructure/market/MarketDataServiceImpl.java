package com.laderrco.fortunelink.portfolio.infrastructure.market;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.infrastructure.exceptions.UnknownSymbolException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.cache.annotation.Cacheable;
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
    if (symbols.isEmpty()) {
      return Map.of();
    }

    // Redis cache hit path (unchanged from before)
    List<String> keys = symbols.stream().map(s -> "market:price::" + s.symbol()).toList();
    List<MarketAssetQuote> cached = redisTemplate.opsForValue().multiGet(keys);

    Map<AssetSymbol, MarketAssetQuote> result = new HashMap<>();
    Set<AssetSymbol> misses = new HashSet<>();
    List<AssetSymbol> symbolList = new ArrayList<>(symbols);

    for (int i = 0; i < symbolList.size(); i++) {
      if (cached != null && cached.get(i) != null) {
        result.put(symbolList.get(i), cached.get(i));
      } else {
        misses.add(symbolList.get(i));
      }
    }

    if (!misses.isEmpty()) {
      // Look up stored currencies for all misses in one DB call,
      // avoiding N individual lookups and eliminating exchange-name guessing
      Map<AssetSymbol, Currency> knownCurrencies = infoRepository.findBySymbols(misses).entrySet()
          .stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().tradingCurrency()));

      Map<AssetSymbol, MarketAssetQuote> fetched = provider.fetchBatchQuotes(misses,
          knownCurrencies);

      Map<String, MarketAssetQuote> toCache = new HashMap<>();
      fetched.forEach((sym, quote) -> {
        result.put(sym, quote);
        toCache.put(cacheKeyPrefix + "::" + sym.symbol(), quote);
      });

      if (!toCache.isEmpty()) {
        redisTemplate.opsForValue().multiSet(toCache);
        toCache.keySet().forEach(k -> redisTemplate.expire(k, Duration.ofSeconds(quoteTtl)));
      }
    }

    return result;
  }

  @Override
  @Cacheable(value = "market:historical", key = "#symbol.symbol()", unless = "#result.isEmpty()")
  public Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date) {
    return provider.fetchHistoricalQuote(symbol, date);
  }

  @Override
  @Cacheable(value = "market:info", key = "#symbol.symbol()", unless = "#result.isEmpty()")
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
  public Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols) {
    if (symbols.isEmpty()) {
      return Map.of();
    }

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

  @Override
  public List<SymbolSearchResult> searchSymbols(String query) {
    return provider.searchSymbols(query);
  }

  @Override
  public MarketAssetInfo validateAndGet(AssetSymbol symbol) {
    return infoRepository.findBySymbol(symbol).orElseGet(() -> {
      MarketAssetInfo external = provider.fetchAssetInfo(symbol)
          .orElseThrow(() -> new UnknownSymbolException(symbol.symbol()));
      infoRepository.save(external);
      return external;
    });
  }

  @Override
  @Cacheable(value = "market:currency", key = "#symbol.symbol()", unless = "#result == null")
  public Currency getTradingCurrency(AssetSymbol symbol) {
    return provider.fetchTradingCurrency(symbol);
  }

  @Override
  public boolean isSymbolSupported(AssetSymbol symbol) {
    return provider.supportsSymbol(symbol);
  }

}