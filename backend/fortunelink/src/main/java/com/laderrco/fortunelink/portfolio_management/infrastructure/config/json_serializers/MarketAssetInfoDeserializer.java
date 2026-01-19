package com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public class MarketAssetInfoDeserializer extends JsonDeserializer<MarketAssetInfo> {
    
    @Override
    public MarketAssetInfo deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        return new MarketAssetInfo(
            node.get("symbol").asText(),
            node.get("name").asText(),
            AssetType.valueOf(node.get("assetType").asText()),
            node.get("exchange").asText(),
            ValidatedCurrency.of(node.get("currency").asText()),
            node.get("sector").asText(),
            node.get("description").asText()
        );
    }
}