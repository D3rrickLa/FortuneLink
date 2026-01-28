package com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public class ValidatedCurrencyDeserializer extends JsonDeserializer<ValidatedCurrency> {
    @Override
    public ValidatedCurrency deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return ValidatedCurrency.of(p.getValueAsString());
    }
}