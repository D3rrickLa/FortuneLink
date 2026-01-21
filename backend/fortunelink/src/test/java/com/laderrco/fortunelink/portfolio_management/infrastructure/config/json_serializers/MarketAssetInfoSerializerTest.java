package com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

class MarketAssetInfoSerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(MarketAssetInfo.class, new MarketAssetInfoSerializer());
        objectMapper.registerModule(module);
    }

    @Test
    void shouldSerializeMarketAssetInfoCorrectly() throws JsonProcessingException {
        MarketAssetInfo assetInfo = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.of("USD"),
                "Technology",
                "Consumer electronics company");

        String json = objectMapper.writeValueAsString(assetInfo);

        assertThat(json).contains("\"symbol\":\"AAPL\"");
        assertThat(json).contains("\"name\":\"Apple Inc.\"");
        assertThat(json).contains("\"assetType\":\"STOCK\"");
        assertThat(json).contains("\"exchange\":\"NASDAQ\"");
        assertThat(json).contains("\"currency\":\"USD\"");
        assertThat(json).contains("\"sector\":\"Technology\"");
        assertThat(json).contains("\"description\":\"Consumer electronics company\"");
    }
}