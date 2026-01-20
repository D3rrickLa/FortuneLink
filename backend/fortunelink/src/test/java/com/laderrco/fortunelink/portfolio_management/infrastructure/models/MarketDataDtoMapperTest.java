package com.laderrco.fortunelink.portfolio_management.infrastructure.models;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class MarketDataDtoMapperTest {
    private MarketDataDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MarketDataDtoMapper();
    }

    // ---------------------------------------------------
    // toPriceResponse
    // ---------------------------------------------------

    @Test
    void toPriceResponse_mapsAllFieldsCorrectly() {
        Money money = new Money(
                BigDecimal.valueOf(123.45),
                ValidatedCurrency.of("USD"));

        MarketAssetQuote quote = new MarketAssetQuote(new SymbolIdentifier("AAPL"), money, money, money, money, money,
                null, null, null, null, Instant.now(), "FMP");

        PriceResponse response = mapper.toPriceResponse("AAPL", quote);

        assertThat(response.getSymbol()).isEqualTo("AAPL");
        assertThat(response.getPrice()).isEqualByComparingTo("123.45");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getSource()).isEqualTo("FMP");

        // dynamic fields
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    // ---------------------------------------------------
    // toPriceResponseMap
    // ---------------------------------------------------

    @Test
    void toPriceResponseMap_convertsMapCorrectly() {
        AssetIdentifier aapl = new SymbolIdentifier("AAPL");
        AssetIdentifier msft = new SymbolIdentifier("MSFT");
        MarketAssetQuote applQuote = new MarketAssetQuote(
                aapl,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "FMP");

        MarketAssetQuote msftQuote = new MarketAssetQuote(
                msft,
                new Money(BigDecimal.valueOf(320), ValidatedCurrency.of("USD")),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "FMP");

        Map<AssetIdentifier, MarketAssetQuote> prices = Map.of(
                aapl, applQuote,
                msft, msftQuote);

        Map<String, PriceResponse> result = mapper.toPriceResponseMap(prices);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("AAPL", "MSFT");

        assertThat(result.get("AAPL").getPrice())
                .isEqualByComparingTo("150");
        assertThat(result.get("MSFT").getCurrency()).isEqualTo("USD");
    }

    // ---------------------------------------------------
    // toAssetInfoResponse
    // ---------------------------------------------------

    @Test
    void toAssetInfoResponse_mapsAllFieldsCorrectly() {
        MarketAssetInfo assetInfo = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.of("USD"),
                "Technology",
                "null");
        // assetInfo.setSector("Technology");

        MarketAssetQuote applQuote = new MarketAssetQuote(
                new SymbolIdentifier("AAPL"),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "FMP");

        AssetInfoResponse response = mapper.toAssetInfoResponse(assetInfo, applQuote);

        assertThat(response.getSymbol()).isEqualTo("AAPL");
        assertThat(response.getName()).isEqualTo("Apple Inc.");
        assertThat(response.getAssetType()).isEqualTo("STOCK");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getExchange()).isEqualTo("NASDAQ");
        assertThat(response.getSector()).isEqualTo("Technology");
        assertThat(response.getSource()).isEqualTo("FMP");
    }

    // ---------------------------------------------------
    // toAssetInfoResponseMap
    // ---------------------------------------------------

    @Test
    void toAssetInfoResponseMap_convertsMapCorrectly() {
        AssetIdentifier btc = new SymbolIdentifier("BTC-USD");

        MarketAssetInfo assetInfo = new MarketAssetInfo(
                "BTC-USD",
                "Bitcoin",
                AssetType.CRYPTO,
                "COINBASE",
                ValidatedCurrency.USD,
                "Crypto",
                null);

        MarketAssetQuote assetQuote = new MarketAssetQuote(
                btc,
                new Money(BigDecimal.valueOf(320), ValidatedCurrency.of("USD")),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "FMP");

        Map<AssetIdentifier, MarketAssetInfo> input = Map.of(btc, assetInfo);

        Map<AssetIdentifier, MarketAssetQuote> quoteMap = Map.of(btc, assetQuote);

        Map<String, AssetInfoResponse> result = mapper.toAssetInfoResponseMap(input, quoteMap);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("BTC-USD");
        assertThat(result.get("BTC-USD").getName()).isEqualTo("Bitcoin");
        assertThat(result.get("BTC-USD").getAssetType()).isEqualTo("CRYPTO");
    }
}
