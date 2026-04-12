package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;

@DisplayName("BocResponseMapper Tests")
class BocResponseMapperTest {

  private BocResponseMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new BocResponseMapper();
  }

  @Nested
  @DisplayName("Primary Mapping Logic (toExchangeRates)")
  class PrimaryMappingTests {

    @Test
    @DisplayName("should skip observation when rates map is null")
    void shouldSkipWhenRatesMapIsNull() {
      // Given: An observation with no rates map at all
      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");
      obs.setRates(null); // Explicit null rates map

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      // When
      List<ExchangeRate> result = mapper.toExchangeRates(response);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should skip specific rate when rate value is null")
    void shouldSkipWhenRateValueIsNull() {
      // Given: An observation with a map containing a null value
      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      Map<String, BocExchangeResponse.Observation.Rate> rateMap = new HashMap<>();
      BocExchangeResponse.Observation.Rate nullRate = new BocExchangeResponse.Observation.Rate();
      nullRate.setValue(null); // Explicit null rate value
      rateMap.put("FXUSDCAD", nullRate);

      obs.setRates(rateMap);

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      // When
      List<ExchangeRate> result = mapper.toExchangeRates(response);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Key Filtering Logic")
  class KeyFilteringTests {

    @Test
    @DisplayName("should skip keys that do not start with FX")
    void shouldSkipKeysNotStartingWithFX() {
      // Given: A non-FX key (e.g., a metadata key or a different series type)
      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate badKey = new BocExchangeResponse.Observation.Rate();
      badKey.setValue(new BigDecimal("1.35"));

      // Map contains a valid key and an invalid key starting with "AB"
      obs.setRates(Map.of(
          "FXUSDCAD", badKey,
          "ABUSDCAD", badKey));

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      // When
      List<ExchangeRate> result = mapper.toExchangeRates(response);

      // Then: Only the "FX" key should be processed
      assertThat(result).hasSize(1);
      assertThat(result.get(0).from().getCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("should skip keys that are not exactly 8 characters long")
    void shouldSkipKeysWithInvalidLength() {
      // Given: Keys that start with FX but are too short or too long
      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate rateValue = new BocExchangeResponse.Observation.Rate();
      rateValue.setValue(new BigDecimal("1.35"));

      // FXUSD (too short), FXUSDCADA (too long)
      obs.setRates(Map.of(
          "FXUSD", rateValue,
          "FXUSDCADA", rateValue,
          "FXUSDCAD", rateValue // The only valid one
      ));

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      // When
      List<ExchangeRate> result = mapper.toExchangeRates(response);

      // Then: Only the 8-character "FXUSDCAD" should remain
      assertThat(result).hasSize(1);
      assertThat(result.get(0).to().getCode()).isEqualTo("CAD");
    }
  }

  @Nested
  @DisplayName("Cross-Currency Mapping (toExchangeRates with Base/Target)")
  class CrossCurrencyTests {

    @Test
    @DisplayName("should return empty when cross-rate components are missing")
    void shouldReturnEmptyWhenTriangulationFails() {
      // Given: We want EUR to USD, but we only have EUR to CAD (missing USD/CAD)
      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate eurCad = new BocExchangeResponse.Observation.Rate();
      eurCad.setValue(new BigDecimal("1.50"));

      // Only EURCAD is present, FXUSDCAD is missing
      obs.setRates(Map.of("FXEURCAD", eurCad));

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      // When
      List<ExchangeRate> result = mapper.toExchangeRates(response, "EUR", "USD");

      // Then: The (baseToCad != null && cadToTarget != null) branch should fail
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should calculate cross-rate when both CAD legs are present")
    void shouldCalculateCrossRateSuccessfully() {
      // Given: We have EUR/CAD and USD/CAD legs
      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate eurRate = new BocExchangeResponse.Observation.Rate();
      eurRate.setValue(new BigDecimal("1.50")); // EUR/CAD = 1.5

      BocExchangeResponse.Observation.Rate usdRate = new BocExchangeResponse.Observation.Rate();
      usdRate.setValue(new BigDecimal("1.25")); // USD/CAD = 1.25 -> CAD/USD = 0.8

      obs.setRates(Map.of("FXEURCAD", eurRate, "FXUSDCAD", usdRate));
      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      // When
      List<ExchangeRate> result = mapper.toExchangeRates(response, "EUR", "USD");

      // Then: 1.5 * 0.8 = 1.2
      assertThat(result).hasSize(1);
      assertThat(result.get(0).rate()).isEqualByComparingTo("1.20");
    }
  }
}