package com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public class ValidatedCurrencySerializer extends JsonSerializer<ValidatedCurrency> {
    @Override
    public void serialize(ValidatedCurrency value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getCode());
    }
}