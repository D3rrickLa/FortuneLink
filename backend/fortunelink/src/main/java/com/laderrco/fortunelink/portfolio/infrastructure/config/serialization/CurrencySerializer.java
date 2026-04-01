package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class CurrencySerializer extends ValueSerializer<Currency> {
  @Override
  public void serialize(Currency value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
    gen.writeString(value.getCode());

  }
}
