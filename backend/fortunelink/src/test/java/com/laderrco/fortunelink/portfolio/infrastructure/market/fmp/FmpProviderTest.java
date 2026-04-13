package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpSearchResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("FMP Provider Integration Tests")
class FmpProviderTest {

  @Mock
  private FmpClient fmpClient;
  @Mock
  private FmpResponseMapper responseMapper;
  @Mock
  private StringRedisTemplate redisTemplate;
  @Mock
  private ValueOperations<String, String> valueOps;

  private FmpProvider fmpProvider;
  private final AssetSymbol aapl = new AssetSymbol("AAPL");

  @BeforeEach
  void setUp() {
    fmpProvider = new FmpProvider(fmpClient, responseMapper, redisTemplate);
    ReflectionTestUtils.setField(fmpProvider, "fmpDailyLimit", 250);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
  }

  @Nested
  @DisplayName("Quota Management (tryReserve)")
  class QuotaTests {

    @Test
    @DisplayName("should return false if Redis increment returns null")
    void shouldReturnFalseOnRedisError() {
      when(valueOps.increment(anyString(), anyLong())).thenReturn(null);
      assertThat(fmpProvider.fetchAssetInfo(aapl)).isEmpty();
    }

    @Test
    @DisplayName("should set expiration on first increment of the day")
    void shouldSetExpirationOnFirstCall() {
      when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);

      fmpProvider.fetchAssetInfo(aapl);

      verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("should return false when daily limit is exceeded")
    void shouldBlockWhenLimitExceeded() {
      when(valueOps.increment(anyString(), anyLong())).thenReturn(251L);

      boolean result = ReflectionTestUtils.invokeMethod(fmpProvider, "tryReserve", 1);

      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Quote Fetching (fetchBatchQuotes)")
  class QuoteTests {

    @Test
    @DisplayName("should return empty map for null or empty input")
    void shouldHandleEmptyInput() {
      assertThat(fmpProvider.fetchBatchQuotes(null, Map.of())).isEmpty();
      assertThat(fmpProvider.fetchBatchQuotes(Set.of(), Map.of())).isEmpty();
    }

    @Test
    @DisplayName("should continue loop if client returns null for a symbol")
    void shouldContinueOnNullRawResponse() {
      when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);
      when(fmpClient.getQuote("AAPL")).thenReturn(null);

      Map<AssetSymbol, MarketAssetQuote> result = fmpProvider.fetchBatchQuotes(Set.of(aapl), Map.of());

      assertThat(result).isEmpty();
      verify(fmpClient).getQuote("AAPL");
    }

    @Test
    @DisplayName("should catch exception during loop and continue")
    void shouldHandleExceptionInLoop() {
      when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);
      when(fmpClient.getQuote(anyString())).thenThrow(new RuntimeException("API Down"));

      Map<AssetSymbol, MarketAssetQuote> result = fmpProvider.fetchBatchQuotes(Set.of(aapl), Map.of());

      assertThat(result).isEmpty(); 
    }
  }

  @Nested
  @DisplayName("Asset Info (fetchAssetInfo & Batch)")
  class AssetInfoTests {

    @Test
    @DisplayName("fetchAssetInfo: should catch exception and return empty")
    void fetchAssetInfo_HandleException() {
      when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);
      when(fmpClient.getProfile(anyString())).thenThrow(new RuntimeException("Error"));

      Optional<MarketAssetInfo> result = fmpProvider.fetchAssetInfo(aapl);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchBatchAssetInfo: should handle duplicates using merge function")
    void fetchBatchAssetInfo_HandleDuplicates() {
      FmpProfileResponse raw = new FmpProfileResponse();
      MarketAssetInfo info = mock(MarketAssetInfo.class);
      when(info.symbol()).thenReturn(aapl);

      
      when(fmpClient.getBatchProfiles(anyList())).thenReturn(List.of(raw, raw));
      when(responseMapper.toAssetInfo(any())).thenReturn(info);

      Map<AssetSymbol, MarketAssetInfo> result = fmpProvider.fetchBatchAssetInfo(Set.of(aapl));

      assertThat(result).hasSize(1); 
    }
  }

  @Nested
  @DisplayName("Helper Methods")
  class HelperTests {

    @Test
    @DisplayName("supportsSymbol: should correctly validate ticker patterns")
    void testSupportsSymbol() {
      assertThat(fmpProvider.supportsSymbol(new AssetSymbol("AAPL"))).isTrue();
      assertThat(fmpProvider.supportsSymbol(new AssetSymbol("BRK.B"))).isTrue();
      
      assertThat(fmpProvider.supportsSymbol(new AssetSymbol("BTC-USD"))).isTrue();
      assertThat(fmpProvider.supportsSymbol(null)).isFalse();
    }

    @Test
    @DisplayName("searchSymbols: should handle client failure gracefully")
    void searchSymbols_HandleFailure() {
      when(fmpClient.getSearch(anyString())).thenThrow(new RuntimeException("Search failed"));

      List<SymbolSearchResult> result = fmpProvider.searchSymbols("test");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchHistoricalQuote: should always return empty")
    void testHistoricalQuoteNotImplemented() {
      assertThat(fmpProvider.fetchHistoricalQuote(aapl, Instant.now())).isEmpty();
    }
  }

  @Nested
  @DisplayName("Specific Branch & Logic Coverage")
  class FmpProviderBranchTests {

    @Test
    @DisplayName("fetchBatchQuotes: should trigger tryReserve and handle null raw response")
    void fetchBatchQuotes_GuardAndNullRaw() {
      
      Set<AssetSymbol> symbols = Set.of(aapl);
      
      when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);
      
      when(fmpClient.getQuote("AAPL")).thenReturn(null);

      
      Map<AssetSymbol, MarketAssetQuote> result = fmpProvider.fetchBatchQuotes(symbols, Map.of());

      
      assertThat(result).isEmpty();
      verify(valueOps).increment(anyString(), eq(1L)); 
    }

    @Test
    @DisplayName("fetchBatchQuotes: should map result when quote is NOT null")
    void fetchBatchQuotes_SuccessPath() {
      
      when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);
      FmpQuoteResponse raw = new FmpQuoteResponse();
      MarketAssetQuote quote = mock(MarketAssetQuote.class);

      when(fmpClient.getQuote("AAPL")).thenReturn(raw);
      
      when(responseMapper.toQuote(eq(raw), any(Currency.class))).thenReturn(quote);

      
      Map<AssetSymbol, MarketAssetQuote> result = fmpProvider.fetchBatchQuotes(Set.of(aapl), Map.of());

      
      assertThat(result).hasSize(1);
      assertThat(result.get(aapl)).isEqualTo(quote);
    }

    @Test
    @DisplayName("fetchBatchAssetInfo: should return empty map for null/empty symbols")
    void fetchBatchAssetInfo_EmptyGuards() {
      assertThat(fmpProvider.fetchBatchAssetInfo(null)).isEmpty();
      assertThat(fmpProvider.fetchBatchAssetInfo(Collections.emptySet())).isEmpty();
    }

    @Test
    @DisplayName("searchSymbols: should return empty for blank query and test success path")
    void searchSymbols_Branches() {
      
      assertThat(fmpProvider.searchSymbols("")).isEmpty();
      assertThat(fmpProvider.searchSymbols(null)).isEmpty();

      
      FmpSearchResponse raw = new FmpSearchResponse();
      SymbolSearchResult mapped = mock(SymbolSearchResult.class);
      when(fmpClient.getSearch("AAPL")).thenReturn(List.of(raw));
      when(responseMapper.toSearchResult(raw)).thenReturn(mapped);

      List<SymbolSearchResult> result = fmpProvider.searchSymbols("AAPL");

      assertThat(result).containsExactly(mapped);
    }

    @Test
    @DisplayName("fetchTradingCurrency: should return currency from info or fallback to USD")
    void fetchTradingCurrency_Logic() {
      when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);

      
      MarketAssetInfo info = mock(MarketAssetInfo.class);
      Currency eur = Currency.of("EUR");
      when(info.tradingCurrency()).thenReturn(eur);
      when(fmpClient.getProfile("AAPL")).thenReturn(new FmpProfileResponse());
      when(responseMapper.toAssetInfo(any())).thenReturn(info);

      assertThat(fmpProvider.fetchTradingCurrency(aapl)).isEqualTo(eur);

      
      when(responseMapper.toAssetInfo(any())).thenReturn(null);
      assertThat(fmpProvider.fetchTradingCurrency(aapl)).isEqualTo(Currency.of("USD"));
    }

    @Test
    @DisplayName("getProviderName: should return FMP")
    void testGetProviderName() {
      assertThat(fmpProvider.getProviderName()).isEqualTo("FMP");
    }

    @Test
    @DisplayName("fetchBatchQuotes: should return empty map when tryReserve fails")
    void fetchBatchQuotes_TryReserveFail() {
      
      when(valueOps.increment(anyString(), anyLong())).thenReturn(251L);

      
      Map<AssetSymbol, MarketAssetQuote> result = fmpProvider.fetchBatchQuotes(Set.of(aapl), Map.of());

      
      assertThat(result).isEmpty();
      
      verifyNoInteractions(fmpClient);
    }

    @Test
    @DisplayName("fetchBatchQuotes: should skip adding to result when mapper returns null")
    void fetchBatchQuotes_MapperReturnsNull() {
      
      when(valueOps.increment(anyString(), anyLong())).thenReturn(10L);

      FmpQuoteResponse raw = new FmpQuoteResponse();
      when(fmpClient.getQuote("AAPL")).thenReturn(raw);

      
      
      when(responseMapper.toQuote(eq(raw), any(Currency.class))).thenReturn(null);

      
      Map<AssetSymbol, MarketAssetQuote> result = fmpProvider.fetchBatchQuotes(Set.of(aapl), Map.of());

      
      assertThat(result).isEmpty();
      assertThat(result).doesNotContainKey(aapl);
    }
  }
}