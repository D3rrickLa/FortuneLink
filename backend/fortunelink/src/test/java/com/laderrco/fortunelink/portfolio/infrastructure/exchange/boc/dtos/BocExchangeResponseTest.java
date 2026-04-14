package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BocExchangeResponse JSON Mapping Tests")
class BocExchangeResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("should correctly map date and dynamic rate keys using @JsonAnySetter")
  void shouldMapDynamicRatesCorrectly() throws Exception {
    // Given: A JSON snippet representing a single observation from BOC
    // Note: "d" is the date, "FXUSDCAD" is a dynamic key, "v" is the value
    String json = """
        {
          "d": "2024-01-01",
          "FXUSDCAD": { "v": "1.3521" },
          "FXEURCAD": { "v": "1.4632" }
        }
        """;

    BocExchangeResponse.Observation observation = objectMapper.readValue(json,
        BocExchangeResponse.Observation.class);

    assertThat(observation.getDate()).isEqualTo("2024-01-01");

    assertThat(observation.getRates()).hasSize(2);
    assertThat(observation.getRates()).containsKey("FXUSDCAD");
    assertThat(observation.getRates()).containsKey("FXEURCAD");

    assertThat(observation.getRates().get("FXUSDCAD").getValue()).isEqualByComparingTo("1.3521");

    assertThat(observation.getRates()).doesNotContainKey("d");
  }

  @Test
  @DisplayName("should handle empty or missing rates gracefully")
  void shouldHandleEmptyRates() throws Exception {

    String json = "{ \"d\": \"2024-01-01\" }";

    BocExchangeResponse.Observation observation = objectMapper.readValue(json,
        BocExchangeResponse.Observation.class);

    assertThat(observation.getDate()).isEqualTo("2024-01-01");
    assertThat(observation.getRates()).isEmpty();
  }

  @Nested
  @DisplayName("Observation addRate Logic")
  class ObservationBranchTests {

    @Test
    @DisplayName("should add rate to map when key is not 'd'")
    void shouldAddRateWhenKeyIsNotD() {
      BocExchangeResponse.Observation observation = new BocExchangeResponse.Observation();
      BocExchangeResponse.Observation.Rate rate = new BocExchangeResponse.Observation.Rate();
      rate.setValue(new BigDecimal("1.2345"));

      observation.addRate("FXUSDCAD", rate);

      assertThat(observation.getRates()).containsKey("FXUSDCAD");
      assertThat(observation.getRates().get("FXUSDCAD")).isEqualTo(rate);
    }

    @Test
    @DisplayName("should skip adding to map when key is 'd'")
    void shouldNotAddRateWhenKeyIsD() {
      BocExchangeResponse.Observation observation = new BocExchangeResponse.Observation();
      BocExchangeResponse.Observation.Rate dummyRate = new BocExchangeResponse.Observation.Rate();

      // When: Manually calling the setter with the forbidden 'd' key
      observation.addRate("d", dummyRate);

      // Then: The map should remain empty
      assertThat(observation.getRates()).isEmpty();
      assertThat(observation.getRates()).doesNotContainKey("d");
    }
  }
}