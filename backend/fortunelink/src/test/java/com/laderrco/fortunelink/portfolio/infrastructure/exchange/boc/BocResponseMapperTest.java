package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");
      obs.setRates(null);

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      List<ExchangeRate> result = mapper.toExchangeRates(response);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should skip specific rate when rate value is null")
    void shouldSkipWhenRateValueIsNull() {

      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      Map<String, BocExchangeResponse.Observation.Rate> rateMap = new HashMap<>();
      BocExchangeResponse.Observation.Rate nullRate = new BocExchangeResponse.Observation.Rate();
      nullRate.setValue(null);
      rateMap.put("FXUSDCAD", nullRate);

      obs.setRates(rateMap);

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      List<ExchangeRate> result = mapper.toExchangeRates(response);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Key Filtering Logic")
  class KeyFilteringTests {

    @Test
    @DisplayName("should skip keys that do not start with FX")
    void shouldSkipKeysNotStartingWithFX() {

      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate badKey = new BocExchangeResponse.Observation.Rate();
      badKey.setValue(new BigDecimal("1.35"));

      obs.setRates(Map.of("FXUSDCAD", badKey, "ABUSDCAD", badKey));

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      List<ExchangeRate> result = mapper.toExchangeRates(response);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).from().getCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("should skip keys that are not exactly 8 characters long")
    void shouldSkipKeysWithInvalidLength() {

      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate rateValue = new BocExchangeResponse.Observation.Rate();
      rateValue.setValue(new BigDecimal("1.35"));

      obs.setRates(Map.of("FXUSD", rateValue, "FXUSDCADA", rateValue, "FXUSDCAD", rateValue));

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      List<ExchangeRate> result = mapper.toExchangeRates(response);

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

      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate eurCad = new BocExchangeResponse.Observation.Rate();
      eurCad.setValue(new BigDecimal("1.50"));

      obs.setRates(Map.of("FXEURCAD", eurCad));

      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      List<ExchangeRate> result = mapper.toExchangeRates(response, "EUR", "USD");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should calculate cross-rate when both CAD legs are present")
    void shouldCalculateCrossRateSuccessfully() {

      BocExchangeResponse.Observation obs = new BocExchangeResponse.Observation();
      obs.setDate("2023-01-01");

      BocExchangeResponse.Observation.Rate eurRate = new BocExchangeResponse.Observation.Rate();
      eurRate.setValue(new BigDecimal("1.50"));

      BocExchangeResponse.Observation.Rate usdRate = new BocExchangeResponse.Observation.Rate();
      usdRate.setValue(new BigDecimal("1.25"));

      obs.setRates(Map.of("FXEURCAD", eurRate, "FXUSDCAD", usdRate));
      BocExchangeResponse response = new BocExchangeResponse();
      response.setObservations(List.of(obs));

      List<ExchangeRate> result = mapper.toExchangeRates(response, "EUR", "USD");

      assertThat(result).hasSize(1);
      assertThat(result.get(0).rate()).isEqualByComparingTo("1.20");
    }
  }
}