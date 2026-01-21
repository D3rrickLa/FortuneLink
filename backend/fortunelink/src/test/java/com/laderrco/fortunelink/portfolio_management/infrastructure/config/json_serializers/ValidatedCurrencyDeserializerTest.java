package com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

class ValidatedCurrencyDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(ValidatedCurrency.class, new ValidatedCurrencyDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    void shouldDeserializeValidCurrency() throws Exception {
        String json = "\"USD\"";

        ValidatedCurrency currency = objectMapper.readValue(json, ValidatedCurrency.class);

        assertThat(currency).isNotNull();
        assertThat(currency.getCode()).isEqualTo("USD");
    }

    @Test
    void shouldFailForInvalidCurrency() {
        String json = "\"INVALID\"";

        assertThatThrownBy(() ->
                objectMapper.readValue(json, ValidatedCurrency.class)
        ).isInstanceOf(RuntimeException.class); // adjust if you throw a specific exception
    }
}