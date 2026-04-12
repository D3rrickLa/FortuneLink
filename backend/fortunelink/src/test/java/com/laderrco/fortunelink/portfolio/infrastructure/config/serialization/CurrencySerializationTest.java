package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencySerializationTest {

  private JsonMapper objectMapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule()
    .addSerializer(Currency.class, new CurrencySerializer())
    .addDeserializer(Currency.class, new CurrencyDeserializer());
    
    objectMapper = JsonMapper.builder().addModule(module).build();
  }

  @Test
  void shouldSerializeCurrencyToString() throws JsonProcessingException {
    // Given
    Currency usd = Currency.of("USD");

    // When
    String json = objectMapper.writeValueAsString(usd);

    // Then
    // Jackson adds quotes around the string: "USD"
    assertThat(json).isEqualTo("\"USD\"");
  }

  @Test
  void shouldDeserializeStringToCurrency() throws JsonProcessingException {
    // Given
    String json = "\"EUR\"";

    // When
    Currency result = objectMapper.readValue(json, Currency.class);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getCode()).isEqualTo("EUR");
  }

  @Test
  void shouldHandleNullDuringSerialization() throws JsonProcessingException {
    // Given
    TestWrapper wrapper = new TestWrapper(null);

    // When
    String json = objectMapper.writeValueAsString(wrapper);

    // Then
    assertThat(json).contains("\"currency\":null");
  }

  @Test
  void shouldHandleNullDuringDeserialization() throws JsonProcessingException {
    // Given
    String json = "{\"currency\":null}";

    // When
    TestWrapper result = objectMapper.readValue(json, TestWrapper.class);

    // Then
    assertThat(result.currency()).isNull();
  }

  // Helper record/class to test null handling in JSON objects
  private record TestWrapper(Currency currency) {
  }
}