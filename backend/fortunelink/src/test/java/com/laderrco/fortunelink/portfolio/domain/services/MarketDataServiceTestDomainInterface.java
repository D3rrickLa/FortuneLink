package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MarketDataServiceTestDomainInterface {

    private final MarketDataService marketDataService = new MarketDataService() {
        @Override
        public Optional<MarketAssetQuote> getCurrentQuote(AssetSymbol symbol) {
            AssetSymbol aapl = new AssetSymbol("AAPL");

            if (aapl.equals(symbol)) {
                MarketAssetQuote quote = new MarketAssetQuote(
                        aapl,
                        new Price(Money.of(123, "USD")),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

                return Optional.of(quote);

            }

            return Optional.empty();
        }

        @Override
        public Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols) {
            return Map.of();
        }

        @Override
        public Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date) {
            return Optional.empty();
        }

        @Override
        public Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol) {
            AssetSymbol aapl = new AssetSymbol("AAPL");

            if (aapl.equals(symbol)) {
                MarketAssetInfo marketAssetInfo = new MarketAssetInfo(
                        aapl,
                        "APPLE",
                        AssetType.STOCK,
                        "NASDAQ",
                        Currency.USD,
                        "Technology",
                        "Apple is an apple company");

                return Optional.of(marketAssetInfo);

            }

            return Optional.empty();
        }

        @Override
        public Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols) {
            return Map.of();
        }

        @Override
        public Currency getTradingCurrency(AssetSymbol symbol) {
            return null;
        }

        @Override
        public boolean isSymbolSupported(AssetSymbol symbol) {
            return false;
        }
    };

    @BeforeEach
    void setUp() {
    }

    @Test
    void getCurrentPrice() {
        Price currentPrice = marketDataService.getCurrentPrice(new AssetSymbol("AAPL"));
        assertEquals(new Price(Money.of(123, "USD")), currentPrice);
    }

    @Test
    void getCurrentPrice_Failure_ThrowsMarketDataException() {
        MarketDataException exception = assertThrows(MarketDataException.class,
                () -> marketDataService.getCurrentPrice(new AssetSymbol("MSFT")));

        assertTrue(exception.getMessage().contains("Price unavailable for: MSFT"));
    }

    @Test
    void testGetAssetInfo_Success() {
        Optional<MarketAssetInfo> info = marketDataService.getAssetInfo(new AssetSymbol("AAPL"));
        assertTrue(info.isPresent());
    }
}