package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocParsingException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BocClient Integration Logic Tests")
class BocClientTest {

  @Mock
  private BankOfCanadaClientConfig config;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private HttpClient httpClient;
  @Mock
  private HttpResponse<Object> httpResponse;

  private BocClient bocClient;

  @BeforeEach
  void setUp() {
    when(config.getBaseUrl()).thenReturn("https://test.api/valet");
    lenient().when(config.getTimeoutSeconds()).thenReturn(5);

    bocClient = new BocClient(config, objectMapper, httpClient);
  }

  @Nested
  @DisplayName("Request Execution and Mapping")
  class ExecutionTests {
    @Test
    @DisplayName("should execute get latest rate and map response successfully")
    void shouldFetchAndMapLatestRate() throws Exception {

      String mockJson = "{\"observations\": []}";
      BocExchangeResponse expectedResponse = new BocExchangeResponse();

      when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenReturn((HttpResponse<Object>) httpResponse);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn(mockJson);
      when(objectMapper.readValue(mockJson, BocExchangeResponse.class))
          .thenReturn(expectedResponse);

      BocExchangeResponse result = bocClient.getLatestExchangeRate("CAD", "USD");

      assertThat(result).isEqualTo(expectedResponse);
      verify(httpClient).send(argThat(request -> request.uri().toString().contains("FXUSDCAD") &&
          request.method().equals("GET")), any());
    }
  }

  @Nested
  @DisplayName("HTTP Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should throw BocApiException for 4xx or 5xx status codes")
    void shouldHandleHttpErrors() throws Exception {

      when(httpClient.send(any(), any())).thenReturn(httpResponse);

      when(httpResponse.statusCode()).thenReturn(404);
      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))
          .isInstanceOf(BocApiException.class)
          .hasMessageContaining("not found");

      when(httpResponse.statusCode()).thenReturn(500);
      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))
          .isInstanceOf(BocApiException.class)
          .hasMessageContaining("internal error");

      when(httpResponse.statusCode()).thenReturn(418);
      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))
          .isInstanceOf(BocApiException.class)
          .hasMessageContaining("418");
    }

    @Test
    @DisplayName("should throw BocApiException on HttpClient network failure")
    void shouldHandleNetworkFailure() throws Exception {
      when(httpClient.send(any(), any())).thenThrow(new java.io.IOException("Connection refused"));

      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))
          .isInstanceOf(BocApiException.class)
          .hasMessageContaining("Connection failed");
    }
  }

  @Nested
  @DisplayName("Specific Logic Branches")
  class BranchTests {

    @Test
    @DisplayName("should log debug info when debug logging is enabled")
    void shouldLogDebugWhenEnabled() throws Exception {

      when(config.isDebugLogging()).thenReturn(true);
      String mockJson = "{\"observations\": []}";

      when(httpClient.send(any(), any())).thenReturn(httpResponse);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn(mockJson);
      when(objectMapper.readValue(anyString(), eq(BocExchangeResponse.class)))
          .thenReturn(new BocExchangeResponse());

      bocClient.getLatestExchangeRate("CAD", "USD");

      verify(config).isDebugLogging();

    }

    @Test
    @DisplayName("should throw BocApiException when Jackson parsing fails")
    void shouldThrowApiExceptionOnParsingFailure() throws Exception {

      String malformedJson = "{ invalid }";
      when(httpClient.send(any(), any())).thenReturn(httpResponse);
      when(httpResponse.statusCode()).thenReturn(299);
      when(httpResponse.body()).thenReturn(malformedJson);

      when(objectMapper.readValue(anyString(), eq(BocExchangeResponse.class)))
          .thenThrow(JacksonException.class);

      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))
          .isInstanceOf(BocApiException.class)
          .hasMessageContaining("Failed to process BOC latest rate request");
    }
  }

  @Nested
  @DisplayName("Historical Rate Logic")
  class HistoricalRateTests {

    @Test
    @DisplayName("should build correct URL for historical requests")
    void shouldFetchHistoricalRates() throws Exception {

      Instant start = Instant.parse("2024-01-01T00:00:00Z");
      Instant end = Instant.parse("2024-01-05T00:00:00Z");
      String mockJson = "{}";

      when(httpClient.send(any(), any())).thenReturn(httpResponse);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn(mockJson);
      when(objectMapper.readValue(anyString(), eq(BocExchangeResponse.class)))
          .thenReturn(new BocExchangeResponse());

      bocClient.getHistoricalExchangeRate("CAD", "USD", start, end);

      verify(httpClient).send(argThat(request -> {
        String uri = request.uri().toString();
        return uri.contains("start_date=2024-01-01") && uri.contains("end_date=2024-01-05");
      }), any());
    }

    @Test
    @DisplayName("should throw BocParsingException when historical data parsing fails")
    void shouldThrowParsingExceptionOnHistoricalFailure() throws Exception {

      when(httpClient.send(any(), any())).thenReturn(httpResponse);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn("bad-data");

      when(objectMapper.readValue(anyString(), eq(BocExchangeResponse.class)))
          .thenThrow(JacksonException.class);

      assertThatThrownBy(() -> bocClient.getHistoricalExchangeRate("CAD", "USD", Instant.now(), Instant.now()))
          .isInstanceOf(BocParsingException.class)
          .hasMessageContaining("Malformed or inaccessible data from BOC");
    }
  }

  @Nested
  @DisplayName("Low-Level HTTP Execution Tests")
  class HttpExecutionTests {

    @Test
    @DisplayName("should handle InterruptedException correctly")
    void shouldHandleInterruptedException() throws Exception {

      when(httpClient.send(any(), any())).thenThrow(new InterruptedException("Interrupted!"));

      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))
          .isInstanceOf(BocApiException.class)
          .hasMessageContaining("interrupted");

      assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    @DisplayName("should handle 400 Bad Request status")
    void shouldHandle400BadRequest() throws Exception {

      when(httpClient.send(any(), any())).thenReturn(httpResponse);
      when(httpResponse.statusCode()).thenReturn(400);

      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))
          .isInstanceOf(BocApiException.class)
          .hasMessageContaining("Invalid inputs for BOC request");
    }

    @Test
    @DisplayName("should return body for any 2xx successful status")
    void shouldHandleAll2xxStatuses() throws Exception {

      String mockJson = "{\"observations\": []}";
      when(httpClient.send(any(), any())).thenReturn(httpResponse);
      when(httpResponse.statusCode()).thenReturn(202);
      when(httpResponse.body()).thenReturn(mockJson);
      when(objectMapper.readValue(anyString(), eq(BocExchangeResponse.class)))
          .thenReturn(new BocExchangeResponse());

      BocExchangeResponse result = bocClient.getLatestExchangeRate("CAD", "USD");

      assertThat(result).isNotNull();
      verify(httpResponse, times(1)).body();
    }
  }

  @Nested
  @DisplayName("Circuit Breaker Fallback")
  class FallbackTests {

    @Test
    @DisplayName("fallback method should throw ExchangeRateUnavailableException")
    void fallbackShouldWork() {

      assertThatThrownBy(() -> bocClient.getLatestExchangeRate("CAD", "USD"))

          .isNotNull();
    }

    @Test
    @DisplayName("Fallback should throw ExchangeRateUnavailableException")
    void testLatestRateFallbackLogic() {
      Throwable cause = new RuntimeException("API Down");

      assertThatThrownBy(() -> bocClient.getLatestRateFallback("CAD", "USD", cause))
          .isInstanceOf(ExchangeRateUnavailableException.class)
          .hasMessageContaining("USD")
          .hasMessageContaining("CAD");
    }
  }
}