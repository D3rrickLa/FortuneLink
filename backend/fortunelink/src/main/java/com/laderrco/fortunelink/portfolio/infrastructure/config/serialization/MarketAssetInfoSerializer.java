package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class MarketAssetInfoSerializer extends ValueSerializer<MarketAssetInfo>{

  @Override
  public void serialize(MarketAssetInfo value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("symbol", value.symbol().symbol());
        gen.writeStringProperty("name", value.name());
        gen.writeStringProperty("assetType", value.type().name());
        gen.writeStringProperty("exchange", value.exchange());
        gen.writeStringProperty("currency", value.tradingCurrency().getCode());
        gen.writeStringProperty("sector", value.sector());
        gen.writeStringProperty("description", value.description());
        gen.writeEndObject();
  }
  
}
