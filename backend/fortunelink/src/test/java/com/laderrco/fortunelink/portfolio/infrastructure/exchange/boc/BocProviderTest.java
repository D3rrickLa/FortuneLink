package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BocProvider Orchestration Tests")
class BocProviderTest {

  private final Currency usd = Currency.of("USD");
  private final Currency cad = Currency.of("CAD");
  @Mock
  private BocClient bocClient;
  @Mock
  private BocResponseMapper mapper;
  private BocProvider bocProvider;

  @BeforeEach
  void setUp() {
    bocProvider = new BocProvider(bocClient, mapper);
  }

  @Test
  @DisplayName("should return identity rate when from and to currencies are same")
  void shouldReturnIdentityForSameCurrency() {
    Instant now = Instant.now();
    ExchangeRate result = bocProvider.getExchangeRate(usd, usd, now);

    assertThat(result.rate()).isEqualByComparingTo("1.0");
    assertThat(result.from()).isEqualTo(usd);
    verifyNoInteractions(bocClient, mapper);
  }

  @Test
  @DisplayName("should throw exception if mapper returns empty list")
  void shouldThrowExceptionWhenMapperReturnsEmpty() {
    Instant now = Instant.now();
    BocExchangeResponse response = new BocExchangeResponse();

    when(bocClient.getLatestExchangeRate(anyString(), anyString())).thenReturn(response);
    when(mapper.toExchangeRates(any(), anyString(), anyString())).thenReturn(List.of());

    assertThatThrownBy(() -> bocProvider.getExchangeRate(usd, cad, now)).isInstanceOf(
        ExchangeRateUnavailableException.class);
  }

  @Nested
  @DisplayName("Latest Rate Logic")
  class LatestRateTests {
    @Test
    @DisplayName("should call latest client when date is today or null")
    void shouldCallLatestClientForToday() {
      Instant now = Instant.now();
      BocExchangeResponse mockResponse = new BocExchangeResponse();
      ExchangeRate mockRate = new ExchangeRate(usd, cad, java.math.BigDecimal.valueOf(1.3), now);

      when(bocClient.getLatestExchangeRate(anyString(), anyString())).thenReturn(mockResponse);
      when(mapper.toExchangeRates(eq(mockResponse), anyString(), anyString())).thenReturn(
          List.of(mockRate));

      ExchangeRate result = bocProvider.getExchangeRate(usd, cad, now);

      assertThat(result).isEqualTo(mockRate);
      verify(bocClient).getLatestExchangeRate(cad.getCode(), usd.getCode());
    }
  }

  @Nested
  @DisplayName("Historical and Fallback Logic")
  class HistoricalFallbackTests {

    @Test
    @DisplayName("should loop up to 7 days back if observations are empty")
    void shouldFallbackUpToSevenDays() {
      Instant asOf = Instant.parse("2024-01-10T10:00:00Z");

      BocExchangeResponse emptyResponse = new BocExchangeResponse();
      emptyResponse.setObservations(Collections.emptyList());

      BocExchangeResponse successResponse = new BocExchangeResponse();
      successResponse.setObservations(List.of(new BocExchangeResponse.Observation()));

      ExchangeRate finalRate = new ExchangeRate(usd, cad, java.math.BigDecimal.ONE, asOf);

      when(bocClient.getHistoricalExchangeRate(eq("USD"), eq("CAD"), any(), any())).thenReturn(
              emptyResponse).thenReturn(emptyResponse).thenReturn(emptyResponse)
          .thenReturn(successResponse);

      when(mapper.toExchangeRates(eq(successResponse), eq("USD"), eq("CAD"))).thenReturn(
          List.of(finalRate));

      bocProvider.getExchangeRate(usd, cad, asOf);

      verify(bocClient, times(4)).getHistoricalExchangeRate(eq("USD"), eq("CAD"), any(), any());
    }

    @Test
    @DisplayName("should throw exception if 7-day fallback is exhausted")
    void shouldThrowExceptionWhenAllFallbacksFail() {
      Instant asOf = Instant.parse("2024-01-10T10:00:00Z");
      BocExchangeResponse emptyResponse = new BocExchangeResponse();
      emptyResponse.setObservations(Collections.emptyList());

      when(bocClient.getHistoricalExchangeRate(anyString(), anyString(), any(), any())).thenReturn(
          emptyResponse);

      assertThatThrownBy(() -> bocProvider.getExchangeRate(usd, cad, asOf)).isInstanceOf(
          ExchangeRateUnavailableException.class);

      verify(bocClient, times(8)).getHistoricalExchangeRate(anyString(), anyString(), any(), any());
    }
  }
}