package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class CurrencyDeserializer extends ValueDeserializer<Currency> {
  @Override
  public Currency deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
    return Currency.of(p.getValueAsString());
  }

}
