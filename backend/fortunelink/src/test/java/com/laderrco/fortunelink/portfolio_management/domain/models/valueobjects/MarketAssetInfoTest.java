package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class MarketAssetInfoTest {
    private MarketAssetInfo testMarketAssetInfo;
    private String symbol;
    private String name;
    private AssetType assetType;
    private String exchange;
    private ValidatedCurrency currency;
    private String sector;
    private Money currentPrice;

    @BeforeEach
    void init() {
        symbol = "AAPL";
        name = "Apple";
        assetType = AssetType.STOCK;
        exchange = "NASDAQ";
        currency = ValidatedCurrency.USD;
        sector = "Technology";
        currentPrice = Money.of(215.55, "USD");
        testMarketAssetInfo = new MarketAssetInfo(symbol, name, assetType, exchange, currency, sector, currentPrice);
    }

    @Test
    void testConstructor() {
        MarketAssetInfo tAssetInfo = new MarketAssetInfo(symbol, name, assetType, exchange, currency, sector, currentPrice);
        assertEquals(tAssetInfo, testMarketAssetInfo);
        assertAll(
            () -> tAssetInfo.getSymbol().equals("AAPL"),
            () -> tAssetInfo.getName().equals("Apple"),
            () -> tAssetInfo.getAssetType().equals(AssetType.STOCK),
            () -> tAssetInfo.getExchange().equals("NASDAQ"),
            () -> tAssetInfo.getCurrency().equals(currency),
            () -> tAssetInfo.getSector().equals("Technology"),
            () -> tAssetInfo.getCurrentPrice().equals(currentPrice)
        );
    }

    @Test 
    void testToIdentifierSucess() {
        MarketIdentifier testIdentifier = new MarketIdentifier(symbol, null, assetType, name, "US$", Map.of("Sector", sector, "Exchange", exchange));
        assertEquals(testIdentifier, testMarketAssetInfo.toIdentifier());
    }

}
