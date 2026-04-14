package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpSearchResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.exceptions.FmpApiException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("FMP Client API Tests")
class FmpClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  private FmpClientConfig config;
  @Mock
  private HttpClient httpClient;
  @Mock
  private HttpResponse<Object> httpResponse;
  private FmpClient fmpClient;

  @BeforeEach
  void setUp() {

    lenient().when(config.getBaseUrl()).thenReturn("https://api.test.com");
    lenient().when(config.getApiKey()).thenReturn("test-key");
    lenient().when(config.getTimeoutSeconds()).thenReturn(10);

    fmpClient = new FmpClient(config, objectMapper, httpClient);
  }

  @Nested
  @DisplayName("Response Parsing Logic")
  class ParsingTests {

    @Test
    @DisplayName("getQuote: should parse FMP array wrapper correctly")
    void shouldParseSingleArrayWrapper() throws Exception {

      String jsonResponse = "[{\"symbol\": \"AAPL\", \"price\": 150.00}]";

      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn(jsonResponse);
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      FmpQuoteResponse result = fmpClient.getQuote("AAPL");

      assertThat(result).isNotNull();
      assertThat(result.getSymbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("getQuote: should return null if FMP returns empty array")
    void shouldHandleEmptyArrayResponse() throws Exception {
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn("[]");
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      assertThat(fmpClient.getQuote("INVALID")).isNull();
    }
  }

  @Nested
  @DisplayName("Error Handling & Status Codes")
  class ErrorTests {

    @Test
    @DisplayName("should throw FmpApiException on 429 Rate Limit")
    void shouldHandleRateLimit() throws Exception {
      when(httpResponse.statusCode()).thenReturn(429);
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      assertThatThrownBy(() -> fmpClient.getQuote("AAPL")).isInstanceOf(FmpApiException.class)
          .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    @DisplayName("should throw FmpApiException on 401 Invalid Key")
    void shouldHandleUnauthorized() throws Exception {
      when(httpResponse.statusCode()).thenReturn(401);
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      assertThatThrownBy(() -> fmpClient.getQuote("AAPL")).isInstanceOf(FmpApiException.class)
          .hasMessageContaining("Invalid FMP API key");
    }

    @Test
    @DisplayName("should handle generic API error with status code")
    void shouldHandleGenericError() throws Exception {
      when(httpResponse.statusCode()).thenReturn(500);
      when(httpResponse.body()).thenReturn("Server Exploded");
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      assertThatThrownBy(() -> fmpClient.getQuote("AAPL")).isInstanceOf(FmpApiException.class)
          .hasMessageContaining("FMP API Error: 500");
    }
  }

  @Nested
  @DisplayName("Batch Processing")
  class BatchTests {

    @Test
    @DisplayName("getBatchQuotes: should filter out nulls and return list")
    void shouldProcessBatchSequentially() throws Exception {

      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn("[{\"symbol\": \"AAPL\"}]").thenReturn("[]");

      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      List<FmpQuoteResponse> results = fmpClient.getBatchQuotes(List.of("AAPL", "MISS"));

      assertThat(results).hasSize(1);
      assertThat(results.get(0).getSymbol()).isEqualTo("AAPL");
      verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    @DisplayName("getBatchQuotes: should return empty list for empty input")
    void shouldReturnEmptyForEmptyInput() {
      assertThat(fmpClient.getBatchQuotes(List.of())).isEmpty();
      assertThat(fmpClient.getBatchQuotes(null)).isEmpty();
    }
  }

  @Nested
  @DisplayName("Resilience Fallbacks")
  class FallbackTests {

    @Test
    @DisplayName("should return null/empty when fallbacks are triggered manually")
    void testFallbackMethods() {
      Throwable t = new RuntimeException("Circuit open");

      assertThat(fmpClient.getQuoteFallback("AAPL", t)).isNull();
      assertThat(fmpClient.getBatchQuotesFallback(List.of("AAPL"), t)).isEmpty();
      assertThat(fmpClient.getProfileFallback("AAPL", t)).isNull();
    }
  }

  @Nested
  @DisplayName("Deep Logic & Branch Coverage")
  class DeepCoverageTests {

    @Test
    @DisplayName("isDebugLogging: should trigger debug log branch")
    void testDebugLoggingBranch() throws Exception {

      when(config.isDebugLogging()).thenReturn(true);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn("[]");
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      fmpClient.getQuote("AAPL");

      verify(config).isDebugLogging();


    }

    @Test
    @DisplayName("handleErrorResponse: should throw 404 specific exception")
    void shouldThrow404Error() throws Exception {

      when(httpResponse.statusCode()).thenReturn(404);
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      assertThatThrownBy(() -> fmpClient.getQuote("AAPL")).isInstanceOf(FmpApiException.class)
          .hasMessageContaining("Endpoint not found");
    }

    @Test
    @DisplayName("executeAndParseList: should handle InterruptedException")
    void shouldHandleInterruptedException() throws Exception {

      when(httpClient.send(any(), any())).thenThrow(new InterruptedException("Interrupted!"));

      assertThatThrownBy(() -> fmpClient.getQuote("AAPL")).isInstanceOf(FmpApiException.class)
          .hasMessageContaining("Request interrupted");

      assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    @DisplayName("executeAndParseList: should handle SocketTimeoutException")
    void shouldHandleTimeoutException() throws Exception {

      when(httpClient.send(any(), any())).thenThrow(new java.net.SocketTimeoutException("Timeout"));

      assertThatThrownBy(() -> fmpClient.getQuote("AAPL")).isInstanceOf(FmpApiException.class)
          .hasMessageContaining("request timed out");
    }

    @Test
    @DisplayName("buildUrl: should handle paths with and without leading slashes")
    void testBuildUrlPathSanitization() throws Exception {

      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn("[]");
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      fmpClient.getQuote("/AAPL");

      fmpClient.getQuote("AAPL");

      when(config.getBaseUrl()).thenReturn("https://api.test.com/");
      fmpClient.getProfile("AAPL");

      verify(config, atLeastOnce()).getBaseUrl();
    }

    @Test
    @DisplayName("getBatchProfiles: should fetch and filter profiles")
    void testGetBatchProfiles() throws Exception {

      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn("[{\"symbol\": \"AAPL\"}]");
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      List<FmpProfileResponse> results = fmpClient.getBatchProfiles(List.of("AAPL"));

      assertThat(results).hasSize(1);
      assertThat(fmpClient.getBatchProfiles(null)).isEmpty();
    }

    @Test
    @DisplayName("getSearch: should build search URL and return list")
    void testGetSearch() throws Exception {

      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn("[{\"symbol\": \"AAPL\", \"name\": \"Apple\"}]");
      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      List<FmpSearchResponse> results = fmpClient.getSearch("Apple");

      assertThat(results).hasSize(1);
      assertThat(results.get(0).getSymbol()).isEqualTo("AAPL");
    }
  }
}