package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FMP Client Configuration Tests")
class FmpClientConfigTest {

  private FmpClientConfig config;

  @BeforeEach
  void setUp() {
    config = new FmpClientConfig();
  }

  @Test
  @DisplayName("should pass validation when a valid API key is provided")
  void shouldPassWithValidKey() {

    config.setApiKey("valid-secret-key-123");

    assertThatCode(() -> config.validate()).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "YOUR_API_KEY"})
  @DisplayName("should throw IllegalStateException for invalid or placeholder keys")
  void shouldFailWithInvalidKeys(String invalidKey) {

    config.setApiKey(invalidKey);

    assertThatThrownBy(() -> config.validate()).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FMP API key is missing");
  }

  @Test
  @DisplayName("should throw IllegalStateException when API key is null")
  void shouldFailWithNullKey() {

    config.setApiKey(null);

    assertThatThrownBy(() -> config.validate()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("should verify default configuration values")
  void shouldHaveCorrectDefaults() {

    assertThat(config.getBaseUrl()).isEqualTo("https://financialmodelingprep.com/api/v3");
    assertThat(config.getTimeoutSeconds()).isEqualTo(10);
    assertThat(config.isDebugLogging()).isFalse();
  }
}