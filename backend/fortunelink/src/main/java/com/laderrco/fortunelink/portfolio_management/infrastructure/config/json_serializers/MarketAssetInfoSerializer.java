package com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;

public class MarketAssetInfoSerializer extends JsonSerializer<MarketAssetInfo> {
    
    @Override
    public void serialize(MarketAssetInfo value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("symbol", value.getSymbol());
        gen.writeStringField("name", value.getName());
        gen.writeStringField("assetType", value.getAssetType().name());
        gen.writeStringField("exchange", value.getExchange());
        gen.writeStringField("currency", value.getCurrency().getCode());
        gen.writeStringField("sector", value.getSector());
        gen.writeStringField("description", value.getDescription());
        gen.writeEndObject();
    }
}