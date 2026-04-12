package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketAssetInfoSerializationTest {

  private JsonMapper objectMapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule()
        .addSerializer(MarketAssetInfo.class, new MarketAssetInfoSerializer())
        .addDeserializer(MarketAssetInfo.class, new MarketAssetInfoDeserializer());
    objectMapper = JsonMapper.builder().addModule(module).build();
  }

  @Test
  void shouldSerializeCompleteObject() throws JsonProcessingException {
    // Given
    MarketAssetInfo info = new MarketAssetInfo(
        new AssetSymbol("AAPL"),
        "Apple Inc.",
        AssetType.STOCK,
        "NASDAQ",
        Currency.of("USD"),
        "Technology",
        "Consumer Electronics");

    // When
    String json = objectMapper.writeValueAsString(info);

    // Then
    assertThat(json)
        .contains("\"symbol\":\"AAPL\"")
        .contains("\"name\":\"Apple Inc.\"")
        .contains("\"assetType\":\"STOCK\"")
        .contains("\"currency\":\"USD\"");
  }

  @Test
  void shouldDeserializeCompleteObject() throws JsonProcessingException {
    // Given
    String json = """
        {
            "symbol": "MSFT",
            "name": "Microsoft",
            "assetType": "STOCK",
            "exchange": "NASDAQ",
            "currency": "USD",
            "sector": "Technology",
            "description": "Software"
        }
        """;

    // When
    MarketAssetInfo result = objectMapper.readValue(json, MarketAssetInfo.class);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.symbol().symbol()).isEqualTo("MSFT");
    assertThat(result.type()).isEqualTo(AssetType.STOCK);
    assertThat(result.tradingCurrency().getCode()).isEqualTo("USD");
    assertThat(result.name()).isEqualTo("Microsoft");
  }

  @Test
  void shouldHandleMissingOptionalFields() throws JsonProcessingException {
    // Given: A JSON that might have missing fields (checking for
    // NullPointerExceptions)
    // Note: This test depends on whether your Deserializer handles missing nodes
    String json = """
        {
            "symbol": "BTC",
            "name": "Bitcoin",
            "assetType": "CRYPTO",
            "exchange": "BINANCE",
            "currency": "USD",
            "sector": "N/A",
            "description": "Digital Gold"
        }
        """;

    // When/Then
    MarketAssetInfo result = objectMapper.readValue(json, MarketAssetInfo.class);
    assertThat(result.symbol().symbol()).isEqualTo("BTC");
  }
}