package com.laderrco.fortunelink.portfolio.infrastructure.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.config.redis.CacheKeyFactory;
import com.laderrco.fortunelink.portfolio.infrastructure.exceptions.UnknownSymbolException;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Integration Logic Tests")
class MarketDataServiceImplTest {

  @Mock
  private MarketDataProvider provider;
  @Mock
  private MarketAssetInfoRepository infoRepository;
  @Mock
  private CacheKeyFactory keyFactory;

  // Redis Mocks
  @Mock
  private RedisTemplate<String, MarketAssetQuote> quoteRedis;
  @Mock
  private RedisTemplate<String, MarketAssetInfo> infoRedis;
  @Mock
  private ValueOperations<String, MarketAssetQuote> quoteOps;
  @Mock
  private ValueOperations<String, MarketAssetInfo> infoOps;

  private MarketDataServiceImpl marketDataService;

  private final AssetSymbol aapl = new AssetSymbol("AAPL");

  @BeforeEach
  void setUp() {
    marketDataService = new MarketDataServiceImpl(
        provider, infoRepository, quoteRedis, infoRedis, keyFactory);

    // Inject @Value fields manually for unit test
    ReflectionTestUtils.setField(marketDataService, "quoteTtl", 60L);
    ReflectionTestUtils.setField(marketDataService, "assetInfoTtl", 3600L);
  }

  @Nested
  @DisplayName("Batch Quotes (getBatchQuotes)")
  class BatchQuoteTests {

    @Test
    @DisplayName("should return cached quotes and fetch misses from provider")
    void shouldHandleCacheHitsAndMisses() {
      // Given
      AssetSymbol msft = new AssetSymbol("MSFT");
      Set<AssetSymbol> symbols = Set.of(aapl, msft);

      MarketAssetQuote aaplQuote = mock(MarketAssetQuote.class);
      MarketAssetInfo msftInfo = mock(MarketAssetInfo.class);
      Currency usd = Currency.of("USD");

      when(keyFactory.price(anyString())).thenAnswer(inv -> "price:" + inv.getArgument(0));

      // Mock Redis: We use a custom answer to ensure the "Hit" follows the key
      when(quoteRedis.opsForValue()).thenReturn(quoteOps);
      when(quoteOps.multiGet(anyList())).thenAnswer(inv -> {
        List<String> requestedKeys = inv.getArgument(0);
        return requestedKeys.stream()
            .map(key -> key.contains("AAPL") ? aaplQuote : null)
            .toList();
      });

      // Mock DB lookup: Use anySet() or verify the specific miss logic
      // We use anySet() here to avoid the Strictness mismatch if order changes
      when(infoRepository.findBySymbols(anySet())).thenReturn(Map.of(msft, msftInfo));
      when(msftInfo.tradingCurrency()).thenReturn(usd);

      // Mock Provider fetch
      MarketAssetQuote msftQuote = mock(MarketAssetQuote.class);
      when(provider.fetchBatchQuotes(anySet(), anyMap())).thenReturn(Map.of(msft, msftQuote));

      // When
      Map<AssetSymbol, MarketAssetQuote> result = marketDataService.getBatchQuotes(symbols);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(aapl)).isEqualTo(aaplQuote);
      assertThat(result.get(msft)).isEqualTo(msftQuote);

      // Verify caching of the new quote only (MSFT)
      verify(quoteOps).multiSet(argThat(map -> map.containsKey("price:MSFT")));
    }

    @Test
    @DisplayName("should return empty map if input set is empty")
    void shouldReturnEmptyForEmptyInput() {
      assertThat(marketDataService.getBatchQuotes(Collections.emptySet())).isEmpty();
      verifyNoInteractions(quoteRedis, provider, infoRepository);
    }

    @Nested
    @DisplayName("Batch Quotes Branch Coverage")
    class BatchQuoteBranchTests {

      @Test
      @DisplayName("should handle null cachedList from Redis safely")
      void shouldHandleNullCachedList() {
        // Given: Redis multiGet returns null (edge case)
        when(quoteRedis.opsForValue()).thenReturn(quoteOps);
        when(quoteOps.multiGet(anyList())).thenReturn(null);

        // Mocking the subsequent calls so it doesn't crash on the 'misses' logic
        when(infoRepository.findBySymbols(anySet())).thenReturn(new HashMap<>());
        when(provider.fetchBatchQuotes(anySet(), anyMap())).thenReturn(new HashMap<>());

        // When
        Map<AssetSymbol, MarketAssetQuote> result = marketDataService.getBatchQuotes(Set.of(aapl));

        // Then
        assertThat(result).isEmpty();
        // Verifies the (cachedList != null) ternary worked
      }

      @Test
      @DisplayName("should execute provider fetch when misses is NOT empty")
      void shouldExecuteWhenMissesNotEmpty() {
        // Given: Cache is empty (misses will contain AAPL)
        when(quoteRedis.opsForValue()).thenReturn(quoteOps);
        when(quoteOps.multiGet(anyList())).thenReturn(Collections.singletonList(null));
        when(keyFactory.price(anyString())).thenReturn("price:AAPL");

        MarketAssetInfo info = mock(MarketAssetInfo.class);
        Currency usd = Currency.of("USD");
        when(info.tradingCurrency()).thenReturn(usd);

        // Mock DB and Provider for the miss
        when(infoRepository.findBySymbols(anySet())).thenReturn(Map.of(aapl, info));
        when(provider.fetchBatchQuotes(anySet(), anyMap())).thenReturn(Map.of(aapl, mock(MarketAssetQuote.class)));

        // When
        marketDataService.getBatchQuotes(Set.of(aapl));

        // Then: Verifies the !misses.isEmpty() block was entered
        verify(infoRepository).findBySymbols(argThat(s -> s.contains(aapl)));
        verify(provider).fetchBatchQuotes(anySet(), anyMap());
      }

      @Test
      @DisplayName("writeQuotesToCache: should skip Redis calls if data map is empty")
      void shouldSkipRedisCallsIfDataIsEmpty() {
        // This branch is triggered when 'fetched' is empty, resulting in an empty
        // 'toCache' map

        // Given: Cache miss, but Provider returns an empty map
        when(quoteRedis.opsForValue()).thenReturn(quoteOps);
        when(quoteOps.multiGet(anyList())).thenReturn(Collections.singletonList(null));
        when(keyFactory.price(anyString())).thenReturn("price:AAPL");

        when(infoRepository.findBySymbols(anySet())).thenReturn(new HashMap<>());
        when(provider.fetchBatchQuotes(anySet(), anyMap())).thenReturn(new HashMap<>()); // Empty return

        // When
        marketDataService.getBatchQuotes(Set.of(aapl));

        // Then: writeQuotesToCache(data) sees data.isEmpty() and returns early
        verify(quoteOps, never()).multiSet(anyMap());
        verify(quoteRedis, never()).expire(anyString(), any(Duration.class));
      }
    }
  }

  @Nested
  @DisplayName("Batch Asset Info (getBatchAssetInfo)")
  class BatchAssetInfoTests {

    @Test
    @DisplayName("should cascade through Cache -> DB -> Provider")
    void shouldCascadeThroughDataSources() {
      // Given
      Set<AssetSymbol> symbols = Set.of(aapl);
      when(keyFactory.assetInfo("AAPL")).thenReturn("info:AAPL");

      // 1. Cache Miss
      when(infoRedis.opsForValue()).thenReturn(infoOps);
      when(infoOps.multiGet(anyList())).thenReturn(Collections.singletonList(null));

      // 2. DB Miss
      when(infoRepository.findBySymbols(anySet())).thenReturn(Map.of());

      // 3. Provider Hit
      MarketAssetInfo info = mock(MarketAssetInfo.class);
      when(provider.fetchBatchAssetInfo(anySet())).thenReturn(Map.of(aapl, info));

      // When
      Map<AssetSymbol, MarketAssetInfo> result = marketDataService.getBatchAssetInfo(symbols);

      // Then
      assertThat(result).containsKey(aapl);
      verify(infoRepository).saveAll(anyMap()); // Verify persistence
      verify(infoOps).multiSet(anyMap()); // Verify caching
    }
  }

  @Nested
  @DisplayName("Asset Info Branch Coverage")
  class AssetInfoBranchTests {

    @Test
    @DisplayName("should return empty map immediately when symbols set is empty")
    void shouldReturnEmptyWhenInputIsEmpty() {
      // When
      Map<AssetSymbol, MarketAssetInfo> result = marketDataService.getBatchAssetInfo(Collections.emptySet());

      // Then
      assertThat(result).isEmpty();
      verifyNoInteractions(infoRedis, infoRepository, provider);
    }

    @Test
    @DisplayName("should handle null cachedList from Redis safely")
    void shouldHandleNullCachedListFromRedis() {
      // Given: Redis ops returns null instead of a list (rare but possible in some
      // drivers)
      when(infoRedis.opsForValue()).thenReturn(infoOps);
      when(infoOps.multiGet(anyList())).thenReturn(null);

      // Setup mocks for subsequent DB/Provider calls so the test can finish
      when(infoRepository.findBySymbols(anySet())).thenReturn(new HashMap<>());
      when(provider.fetchBatchAssetInfo(anySet())).thenReturn(new HashMap<>());

      // When & Then
      // This confirms the (cachedList != null) ternary doesn't crash
      Map<AssetSymbol, MarketAssetInfo> result = marketDataService.getBatchAssetInfo(Set.of(aapl));
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should skip DB and Provider blocks if all items are found in cache")
    void shouldSkipFallbacksIfAllCached() {
      // Given: Everything is in cache
      MarketAssetInfo info = mock(MarketAssetInfo.class);
      when(infoRedis.opsForValue()).thenReturn(infoOps);
      when(infoOps.multiGet(anyList())).thenReturn(List.of(info));
      when(keyFactory.assetInfo(anyString())).thenReturn("key");

      // When
      marketDataService.getBatchAssetInfo(Set.of(aapl));

      // Then: Verify !misses.isEmpty() blocks were skipped
      verify(infoRepository, never()).findBySymbols(anySet());
      verify(provider, never()).fetchBatchAssetInfo(anySet());
    }

    @Test
    @DisplayName("should skip persistence if provider returns empty map")
    void shouldSkipSaveIfProviderReturnsEmpty() {
      // Given: Cache miss, DB miss, and Provider also returns nothing
      when(infoRedis.opsForValue()).thenReturn(infoOps);
      when(infoOps.multiGet(anyList())).thenReturn(Collections.singletonList(null));
      when(infoRepository.findBySymbols(anySet())).thenReturn(new HashMap<>());
      when(provider.fetchBatchAssetInfo(anySet())).thenReturn(new HashMap<>()); // Empty return

      // When
      marketDataService.getBatchAssetInfo(Set.of(aapl));

      // Then: !fetched.isEmpty() branch is skipped
      verify(infoRepository, never()).saveAll(anyMap());
    }

    @Test
    @DisplayName("should catch and log exception when database save fails")
    void shouldHandleDatabaseSaveFailureGracefully() {
      // Given: Everything is a miss, Provider returns data, but DB save crashes
      MarketAssetInfo info = mock(MarketAssetInfo.class);
      when(infoRedis.opsForValue()).thenReturn(infoOps);
      when(infoOps.multiGet(anyList())).thenReturn(Collections.singletonList(null));
      when(infoRepository.findBySymbols(anySet())).thenReturn(new HashMap<>());
      when(provider.fetchBatchAssetInfo(anySet())).thenReturn(Map.of(aapl, info));

      // Force the saveAll to crash
      doThrow(new RuntimeException("DB Error")).when(infoRepository).saveAll(anyMap());

      // When
      Map<AssetSymbol, MarketAssetInfo> result = marketDataService.getBatchAssetInfo(Set.of(aapl));

      // Then
      assertThat(result).containsKey(aapl); // Service should still return data even if save failed
      verify(infoRepository).saveAll(anyMap());
      // Verify code continues to cache even after DB failure
      verify(infoOps).multiSet(anyMap());
    }
  }

  @Nested
  @DisplayName("Historical Quotes")
  class HistoricalQuoteTests {

    @Test
    @DisplayName("should return cached historical quote if present")
    void shouldReturnCachedHistorical() {
      Instant now = Instant.now();
      MarketAssetQuote quote = mock(MarketAssetQuote.class);
      when(keyFactory.historical(anyString(), any())).thenReturn("hist:key");
      when(quoteRedis.opsForValue()).thenReturn(quoteOps);
      when(quoteOps.get("hist:key")).thenReturn(quote);

      Optional<MarketAssetQuote> result = marketDataService.getHistoricalQuote(aapl, now);

      assertThat(result).isPresent().contains(quote);
      verifyNoInteractions(provider);
    }

    @Nested
    @DisplayName("Historical and Passthrough Tests")
    class HistoricalAndPassthroughTests {

      @Test
      @DisplayName("getHistoricalQuote: should fetch from provider and cache when cache is null")
      void getHistoricalQuoteCacheMiss() {
        // Given
        Instant date = Instant.now();
        String key = "hist:AAPL:" + date;
        MarketAssetQuote quote = mock(MarketAssetQuote.class);

        when(keyFactory.historical(anyString(), any())).thenReturn(key);
        when(quoteRedis.opsForValue()).thenReturn(quoteOps);

        // 1. Force Cache Miss (cached == null)
        when(quoteOps.get(key)).thenReturn(null);

        // 2. Provider returns data
        when(provider.fetchHistoricalQuote(aapl, date)).thenReturn(Optional.of(quote));

        // When
        Optional<MarketAssetQuote> result = marketDataService.getHistoricalQuote(aapl, date);

        // Then
        assertThat(result).isPresent().contains(quote);

        // 3. Verify the lambda inside ifPresent() fired and cached the result
        verify(quoteOps).set(eq(key), eq(quote), any(Duration.class));
      }

      @Test
      @DisplayName("getTradingCurrency: should use provider fallback when getAssetInfo is empty")
      void getTradingCurrencyDoubleFallback() {
        // Given: getAssetInfo returns Optional.empty()
        // We do this by making all cascading lookups fail
        when(infoRedis.opsForValue()).thenReturn(infoOps);
        when(infoOps.multiGet(anyList())).thenReturn(Collections.singletonList(null));
        when(infoRepository.findBySymbols(anySet())).thenReturn(Map.of());
        when(provider.fetchBatchAssetInfo(anySet())).thenReturn(Map.of());

        // Mock the final provider fallback
        Currency eur = Currency.of("EUR");
        when(provider.fetchTradingCurrency(aapl)).thenReturn(eur);

        // When
        Currency result = marketDataService.getTradingCurrency(aapl);

        // Then
        assertThat(result).isEqualTo(eur);
        verify(provider).fetchTradingCurrency(aapl); // Confirms orElseGet lambda fired
      }

      @Test
      @DisplayName("searchSymbols: should passthrough to provider")
      void searchSymbolsPassthrough() {
        List<SymbolSearchResult> expected = List.of(mock(SymbolSearchResult.class));
        when(provider.searchSymbols("AAPL")).thenReturn(expected);

        List<SymbolSearchResult> result = marketDataService.searchSymbols("AAPL");

        assertThat(result).isEqualTo(expected);
      }

      @Test
      @DisplayName("isSymbolSupported: should passthrough to provider")
      void isSymbolSupportedPassthrough() {
        when(provider.supportsSymbol(aapl)).thenReturn(true);

        boolean result = marketDataService.isSymbolSupported(aapl);

        assertThat(result).isTrue();
        verify(provider).supportsSymbol(aapl);
      }
    }
  }

  @Nested
  @DisplayName("Validation and Helpers")
  class ValidationTests {

    @Test
    @DisplayName("validateAndGet should throw exception if symbol not found")
    void validateAndGetShouldThrow() {
      // Mock empty results from the cascade
      when(infoRedis.opsForValue()).thenReturn(infoOps);
      when(infoOps.multiGet(anyList())).thenReturn(Collections.singletonList(null));
      when(infoRepository.findBySymbols(anySet())).thenReturn(Map.of());
      when(provider.fetchBatchAssetInfo(anySet())).thenReturn(Map.of());

      assertThatThrownBy(() -> marketDataService.validateAndGet(aapl))
          .isInstanceOf(UnknownSymbolException.class);
    }
  }
}