package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;


import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

class CurrencySerializationTest {

  private JsonMapper objectMapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule().addSerializer(Currency.class, new CurrencySerializer())
        .addDeserializer(Currency.class, new CurrencyDeserializer());

    objectMapper = JsonMapper.builder().addModule(module).build();
  }

  @Test
  void shouldSerializeCurrencyToString() throws JsonProcessingException {

    Currency usd = Currency.of("USD");

    String json = objectMapper.writeValueAsString(usd);

    assertThat(json).isEqualTo("\"USD\"");
  }

  @Test
  void shouldDeserializeStringToCurrency() throws JsonProcessingException {

    String json = "\"EUR\"";

    Currency result = objectMapper.readValue(json, Currency.class);

    assertThat(result).isNotNull();
    assertThat(result.getCode()).isEqualTo("EUR");
  }

  @Test
  void shouldHandleNullDuringSerialization() throws JsonProcessingException {

    TestWrapper wrapper = new TestWrapper(null);

    String json = objectMapper.writeValueAsString(wrapper);

    assertThat(json).contains("\"currency\":null");
  }

  @Test
  void shouldHandleNullDuringDeserialization() throws JsonProcessingException {

    String json = "{\"currency\":null}";

    TestWrapper result = objectMapper.readValue(json, TestWrapper.class);

    assertThat(result.currency()).isNull();
  }


  private record TestWrapper(Currency currency) {
  }
}