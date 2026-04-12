package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BankOfCanadaClientConfigTest {

  private BankOfCanadaClientConfig config;

  @BeforeEach
  void setUp() {
    config = new BankOfCanadaClientConfig();
  }

  @Nested
  @DisplayName("Validation Logic")
  class ValidationTests {

    @Test
    @DisplayName("should pass validation with default URL")
    void shouldPassWithDefaultUrl() {
      // Should not throw any exception
      config.validate();
    }

    @Test
    @DisplayName("should throw IllegalStateException when URL is null")
    void shouldFailWhenUrlIsNull() {
      config.setBaseUrl(null);

      assertThatThrownBy(() -> config.validate())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("cannot be blank");
    }

    @Test
    @DisplayName("should throw IllegalStateException when URL is blank")
    void shouldFailWhenUrlIsBlank() {
      config.setBaseUrl("   ");

      assertThatThrownBy(() -> config.validate())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("cannot be blank");
    }
  }

  @Nested
  @DisplayName("Bean Creation logic")
  class BeanCreationTests {

    @Test
    @DisplayName("should create HttpClient with configured timeout")
    void shouldCreateHttpClient() {
      // Given
      config.setTimeoutSeconds(5);

      // When
      HttpClient client = config.bocHttpClient(config);

      // Then
      assertThat(client).isNotNull();
      assertThat(client.connectTimeout()).isPresent().contains(Duration.ofSeconds(5));
      assertThat(client.followRedirects()).isEqualTo(HttpClient.Redirect.NORMAL);
    }
  }
}