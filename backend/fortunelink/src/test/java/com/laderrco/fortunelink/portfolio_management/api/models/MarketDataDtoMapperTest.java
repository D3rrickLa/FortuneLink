package com.laderrco.fortunelink.portfolio_management.api.models;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.api.models.market.AssetInfoResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.market.MarketDataDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.market.PriceResponse;
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

    @DisplayName("toAssetInfoResponse: should set quote-derived fields to null when quote is null")
    @Test
    void toAssetInfoResponse_ShouldSetNulls_WhenQuoteIsNull() {

        // given
        MarketAssetInfo info = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.USD,
                "Technology",
                "Consumer electronics company");

        MarketAssetQuote quote = null;

        // when
        AssetInfoResponse response = mapper.toAssetInfoResponse(info, quote);

        // then
        assertThat(response).isNotNull();

        // info-derived fields
        assertThat(response.getSymbol()).isEqualTo("AAPL");
        assertThat(response.getName()).isEqualTo("Apple Inc.");
        assertThat(response.getAssetType()).isEqualTo("STOCK");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getExchange()).isEqualTo("NASDAQ");
        assertThat(response.getSector()).isEqualTo("Technology");
        assertThat(response.getDescription()).isEqualTo("Consumer electronics company");

        // quote-derived fields (must be null)
        assertThat(response.getCurrentPrice()).isNull();
        assertThat(response.getMarketCap()).isNull();
        assertThat(response.getTimestamp()).isNull();
        assertThat(response.getSource()).isNull();
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

    @DisplayName("toAssetInfoResponseMap: should return map and set quote fields to null when quoteMap is null")
    @Test
    void toAssetInfoResponseMap_ShouldReturnMap_WhenQuoteMapIsNull() {

        // given
        AssetIdentifier assetId = SymbolIdentifier.of("AAPL");

        MarketAssetInfo info = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.USD,
                "Technology",
                "Consumer electronics company");

        Map<AssetIdentifier, MarketAssetInfo> infoMap = Map.of(assetId, info);

        Map<AssetIdentifier, MarketAssetQuote> quoteMap = null;

        // when
        Map<String, AssetInfoResponse> result = mapper.toAssetInfoResponseMap(infoMap, quoteMap);

        // then
        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsKey("AAPL");

        AssetInfoResponse response = result.get("AAPL");

        // info-derived fields
        assertThat(response.getSymbol()).isEqualTo("AAPL");
        assertThat(response.getName()).isEqualTo("Apple Inc.");
        assertThat(response.getAssetType()).isEqualTo("STOCK");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getExchange()).isEqualTo("NASDAQ");
        assertThat(response.getSector()).isEqualTo("Technology");
        assertThat(response.getDescription()).isEqualTo("Consumer electronics company");

        // quote-derived fields must be null
        assertThat(response.getCurrentPrice()).isNull();
        assertThat(response.getMarketCap()).isNull();
        assertThat(response.getTimestamp()).isNull();
        assertThat(response.getSource()).isNull();
    }

    @DisplayName("toAssetInfoResponseMap: should keep first quote when duplicate cache keys exist")
    @Test
    void toAssetInfoResponseMap_ShouldUseFirstQuote_WhenDuplicateQuoteCacheKeys() {
        class TestSybId implements AssetIdentifier {

            private String syb;

            public TestSybId(String syb) {
                this.syb = syb;
            }

            @Override
            public String getPrimaryId() {
                return "AAPL";
            }

            @Override
            public String displayName() {
                return syb;
            }

            @Override
            public AssetType getAssetType() {
                return null;
            }
            public String cacheKey() {
                return "AAPL";
            }

        }
        AssetIdentifier assetId = new TestSybId("AAPL");
        MarketAssetInfo info = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.USD,
                "Technology",
                "Consumer electronics company");

        MarketAssetQuote firstQuote = mockQuote("AAPL", BigDecimal.TEN);
        MarketAssetQuote secondQuote = mockQuote("AAPL", BigDecimal.ONE);

        Map<AssetIdentifier, MarketAssetInfo> infoMap = Map.of(assetId, info);

        Map<AssetIdentifier, MarketAssetQuote> quoteMap = new LinkedHashMap<>();
        quoteMap.put(SymbolIdentifier.of("AAPL-1"), firstQuote);
        quoteMap.put(SymbolIdentifier.of("AAPL-2"), secondQuote);

        Map<String, AssetInfoResponse> result = mapper.toAssetInfoResponseMap(infoMap, quoteMap);

        AssetInfoResponse response = result.get("AAPL");

        assertThat(response.getCurrentPrice()).isEqualTo(BigDecimal.TEN);
    }

    @DisplayName("toAssetInfoResponseMap: should keep first asset when duplicate primaryIds exist")
    @Test
    void toAssetInfoResponseMap_ShouldUseFirstAsset_WhenDuplicatePrimaryIds() {

        class TestSybId implements AssetIdentifier {

            private String syb;

            public TestSybId(String syb) {
                this.syb = syb;
            }

            @Override
            public String getPrimaryId() {
                return "AAPL";
            }

            @Override
            public String displayName() {
                return syb;
            }

            @Override
            public AssetType getAssetType() {
                return null;
            }

        }

        AssetIdentifier id1 = new TestSybId("AAPL-1");

        AssetIdentifier id2 = new TestSybId("AAPL-2");

        MarketAssetInfo info1 = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.USD,
                "Tech",
                "First");

        MarketAssetInfo info2 = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.USD,
                "Tech",
                "Second");

        Map<AssetIdentifier, MarketAssetInfo> infoMap = new LinkedHashMap<>();
        infoMap.put(id1, info1);
        infoMap.put(id2, info2); // keys are distinct objects

        Map<String, AssetInfoResponse> result = mapper.toAssetInfoResponseMap(infoMap, null);

        assertThat(result).hasSize(1);

        AssetInfoResponse response = result.get("AAPL");

        // First entry wins
        assertThat(response.getDescription()).isEqualTo("First");
    }

    private MarketAssetQuote mockQuote(String cacheKey, BigDecimal amount) {
        MarketAssetQuote quote = mock(MarketAssetQuote.class);
        SymbolIdentifier id = mock(SymbolIdentifier.class);
        Money money = mock(Money.class);

        when(id.cacheKey()).thenReturn(cacheKey);
        when(quote.id()).thenReturn(id);

        // return Money object
        when(quote.currentPrice()).thenReturn(money);
        when(money.amount()).thenReturn(amount);
        when(money.currency()).thenReturn(ValidatedCurrency.USD);

        return quote;
    }

}
