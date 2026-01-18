package com.laderrco.fortunelink.portfolio_management.infrastructure.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.ErrorType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.DevSecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.SymbolNotFoundException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.AssetInfoResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.MarketDataDtoMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.PriceResponse;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@AutoConfigureMockMvc
@Import(DevSecurityConfig.class) // Explicitly pulls in your permitAll() logic
@WebMvcTest(MarketDataController.class)
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketDataService marketDataService;

    @MockitoBean
    private MarketDataDtoMapper mapper;

    // ---------------------------------------------------
    // /price/{symbol}
    // ---------------------------------------------------

    @Test
    void getCurrentPrice_success() throws Exception {
        SymbolIdentifier symbolId = new SymbolIdentifier("AAPL");
        Money price = new Money(BigDecimal.valueOf(150.25), ValidatedCurrency.of("USD"));
        PriceResponse response = PriceResponse.of("AAPL", BigDecimal.valueOf(150.25), "USD");

        when(marketDataService.getCurrentPrice(symbolId)).thenReturn(price);
        when(mapper.toPriceResponse("AAPL", price)).thenReturn(response);

        mockMvc.perform(get("/api/market-data/price/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(150.25))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    // ---------------------------------------------------
    // /prices?symbols=AAPL,GOOGL
    // ---------------------------------------------------

    @Test
    void getBatchPrices_success() throws Exception {
        AssetIdentifier aapl = new SymbolIdentifier("AAPL");
        AssetIdentifier googl = new SymbolIdentifier("GOOGL");

        Map<AssetIdentifier, Money> prices = Map.of(
                aapl, new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                googl, new Money(BigDecimal.valueOf(2800), ValidatedCurrency.of("USD")));

        Map<String, PriceResponse> response = Map.of(
                "AAPL", PriceResponse.of("AAPL", BigDecimal.valueOf(150), "USD"),
                "GOOGL",PriceResponse.of("GOOGL", BigDecimal.valueOf(2800), "USD"));

        when(marketDataService.getBatchPrices(anyList())).thenReturn(prices);
        when(mapper.toPriceResponseMap(prices)).thenReturn(response);

        mockMvc.perform(get("/api/market-data/prices")
                .param("symbols", "AAPL", "GOOGL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.AAPL.price").value(150))
                .andExpect(jsonPath("$.GOOGL.price").value(2800));
    }

    // ---------------------------------------------------
    // /asset-info/{symbol}
    // ---------------------------------------------------

    @Test
    void getAssetInfo_success() throws Exception {
        SymbolIdentifier symbolId = new SymbolIdentifier("AAPL");

        MarketAssetInfo assetInfo = new MarketAssetInfo(
            "AAPL",
            "Apple Inc.",
            AssetType.STOCK,
            "NASDAQ",
            ValidatedCurrency.USD,
            "Technology",
            "DESC"
        );

        AssetInfoResponse response = new AssetInfoResponse(
            "AAPL",
            "Apple Inc.",
            "STOCK",
            "USD",
            "NASDAQ",
            BigDecimal.valueOf(235.66),
            "Technology",
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            "INTERNAL"
        );

        when(marketDataService.getAssetInfo(symbolId))
                .thenReturn(Optional.of(assetInfo));
        when(mapper.toAssetInfoResponse(assetInfo)).thenReturn(response);

        mockMvc.perform(get("/api/market-data/asset-info/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.currency").value("USD"));

    }

    @Test
    void getAssetInfo_notFound() throws Exception {
        when(marketDataService.getAssetInfo(anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/market-data/asset-info/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------
    // /asset-info (batch)
    // ---------------------------------------------------

    @Test
    void getBatchAssetInfo_success() throws Exception {
        AssetIdentifier aapl = new SymbolIdentifier("AAPL");

        MarketAssetInfo assetInfo =new MarketAssetInfo(
            "AAPL",
            "Apple Inc.",
            AssetType.STOCK,
            "NASDAQ",
            ValidatedCurrency.USD,
            "Technology",
            "DESC"
        );

        Map<AssetIdentifier, MarketAssetInfo> serviceResult = Map.of(aapl, assetInfo);

        Map<String, AssetInfoResponse> response = Map.of("AAPL", new AssetInfoResponse(
            "AAPL",
            "Apple Inc.",
            "STOCK",
            "USD",
            "NASDAQ",
            BigDecimal.valueOf(235.66),
            "Technology",
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            "INTERNAL"
        ));

        when(marketDataService.getBatchAssetInfo(anyList()))
                .thenReturn(serviceResult);
        when(mapper.toAssetInfoResponseMap(serviceResult))
                .thenReturn(response);

        mockMvc.perform(get("/api/market-data/asset-info")
                .param("symbols", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.AAPL.name").value("Apple Inc."));
    }

    // ---------------------------------------------------
    // /currency/{symbol}
    // ---------------------------------------------------

    @Test
    void getTradingCurrency_success() throws Exception {
        when(marketDataService.getTradingCurrency(any()))
                .thenReturn(ValidatedCurrency.of("USD"));

        mockMvc.perform(get("/api/market-data/currency/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("USD"));
    }

    // ---------------------------------------------------
    // /supported/{symbol}
    // ---------------------------------------------------

    @Test
    void isSymbolSupported_true() throws Exception {
        when(marketDataService.isSymbolSupported(any()))
                .thenReturn(true);

        mockMvc.perform(get("/api/market-data/supported/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void isSymbolSupported_false() throws Exception {
        when(marketDataService.isSymbolSupported(any()))
                .thenReturn(false);

        mockMvc.perform(get("/api/market-data/supported/XYZ"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ---------------------------------------------------
    // /health
    // ---------------------------------------------------

    @Test
    void healthCheck_up() throws Exception {
        when(marketDataService.getCurrentPrice(any()))
                .thenReturn(new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")));

        mockMvc.perform(get("/api/market-data/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("market-data"))
                .andExpect(jsonPath("$.testSymbol").value("AAPL"));
    }

    @Test
    void healthCheck_down() throws Exception {
        when(marketDataService.getCurrentPrice(any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(get("/api/market-data/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.service").value("market-data"));
    }

    @Test
    void getPrice_ShouldReturn404_WhenSymbolNotFound() throws Exception {
        // Arrange: Force the service to throw the specific exception
        String symbol = "INVALID";
        when(marketDataService.getCurrentPrice(any()))
                .thenThrow(new SymbolNotFoundException("Symbol not found: " + symbol));

        // Act & Assert
        mockMvc.perform(get("/api/market-data/price/{symbol}", symbol))
                .andExpect(status().isNotFound()) // 404
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Symbol not found: INVALID"))
                .andExpect(jsonPath("$.path").value("/api/market-data/price/INVALID"));
    }

    @Test
    void getPrice_ShouldReturn503_WhenMarketDataFails() throws Exception {
        // Arrange: Simulate a provider timeout or rate limit
        when(marketDataService.getCurrentPrice(any()))
                .thenThrow(new MarketDataException("API rate limit exceeded", ErrorType.RATE_LIMIT_EXCEEDED));

        // Act & Assert
        mockMvc.perform(get("/api/market-data/price/AAPL"))
                .andExpect(status().isServiceUnavailable()) // 503
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                // Note: Your handler overrides the message for 503s for security
                .andExpect(jsonPath("$.message")
                        .value("Market data service is temporarily unavailable. Please try again later."))
                .andExpect(jsonPath("$.details").value("API rate limit exceeded"));
    }

    @Test
    void getPrice_ShouldReturn400_WhenIllegalArgumentThrown() throws Exception {
        // Arrange: Simulate bad input validation in the service
        when(marketDataService.getCurrentPrice(any()))
                .thenThrow(new IllegalArgumentException("Invalid symbol format"));

        // Act & Assert
        mockMvc.perform(get("/api/market-data/price/!!!"))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid symbol format"));
    }
}