package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public class MarketAssetInfoDeserializer extends ValueDeserializer<MarketAssetInfo> {
  @Override
  public MarketAssetInfo deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
    JsonNode node = p.objectReadContext().readTree(p);

    return new MarketAssetInfo(
        new AssetSymbol(node.get("symbol").asString()),
        node.get("name").asString(),
        AssetType.valueOf(node.get("assetType").asString()),
        node.get("exchange").asString(),
        Currency.of(node.get("currency").asString()),
        node.get("sector").asString(),
        node.get("description").asString());
  }

}
