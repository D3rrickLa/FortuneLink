package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.laderrco.fortunelink.portfolio_management.api.models.market.AssetInfoResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.market.MarketDataDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.market.PriceResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.SymbolNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API for market data operations.
 * Provides endpoints for retrieving asset prices, info, and metadata.
 * 
 * Rate Limiting Strategy:
 * - Use batch endpoints when fetching multiple symbols
 * - Single symbol endpoints for real-time price checks
 * - Consider caching frequently accessed symbols
 */
@Slf4j
@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
@Tag(name = "Market Data", description = "Endpoints for retrieving market prices and asset information")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final MarketDataDtoMapper mapper;

    /**
     * Get current price for a single asset.
     * 
     * Example: GET /api/market-data/price/AAPL
     */
    @GetMapping("/price/{symbol}")
    @Operation(summary = "Get current price for a symbol", description = "Retrieves the current market price for the specified asset symbol")
    @ApiResponse(responseCode = "200", description = "Price retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Symbol not found")
    @ApiResponse(responseCode = "503", description = "Market data service unavailable")
    public ResponseEntity<PriceResponse> getCurrentPrice(
            @Parameter(description = "Asset symbol (e.g., AAPL, BTC-USD)", example = "AAPL") @PathVariable String symbol) {
        log.debug("Fetching current price for symbol: {}", symbol);
        SymbolIdentifier symbolId = SymbolIdentifier.of(symbol);

        MarketAssetQuote quote = marketDataService
                .getCurrentQuote(symbolId)
                .orElseThrow(() -> new SymbolNotFoundException("Symbol not found: " + symbol));

        PriceResponse response = mapper.toPriceResponse(symbol, quote);

        log.info("Successfully retrieved price for {}: {} {}", symbol, quote.currentPrice().amount(),
                quote.currentPrice().currency());
        return ResponseEntity.ok(response);
    }

    /**
     * Get current prices for multiple assets (batch operation).
     * More efficient than calling /price/{symbol} multiple times.
     * 
     * Example: GET /api/market-data/prices?symbols=AAPL,GOOGL,MSFT
     */
    @GetMapping("/prices")
    @Operation(summary = "Get current prices for multiple symbols (batch)", description = "Retrieves current market prices for multiple symbols in a single request. More efficient than individual requests.")
    @ApiResponse(responseCode = "200", description = "Prices retrieved successfully")
    public ResponseEntity<Map<String, PriceResponse>> getBatchPrices(
            @Parameter(description = "Comma-separated list of symbols", example = "AAPL,GOOGL,MSFT") @RequestParam List<String> symbols) {
        log.debug("Fetching batch prices for {} symbols: {}", symbols.size(), symbols);

        List<SymbolIdentifier> ids = symbols.stream()
                .map(this::symbol)
                .toList();
        Map<AssetIdentifier, MarketAssetQuote> quotes = marketDataService.getBatchQuotes(ids);

        log.debug("Successfully retrieved {} prices", quotes.size());
        return ResponseEntity.ok(mapper.toPriceResponseMap(quotes));

    }

    /**
     * Get detailed asset information (name, type, currency, exchange, etc.).
     * 
     * Example: GET /api/market-data/asset-info/AAPL
     */
    @GetMapping("/asset-info/{symbol}")
    @Operation(summary = "Get detailed asset information", description = "Retrieves comprehensive information about an asset including name, type, currency, exchange, etc.")
    @ApiResponse(responseCode = "200", description = "Asset info retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Symbol not found")
    public ResponseEntity<AssetInfoResponse> getAssetInfo(@Parameter(description = "Asset symbol", example = "AAPL") @PathVariable String symbol) {
        log.debug("Fetching asset info for symbol: {}", symbol);

        MarketAssetInfo assetInfo = marketDataService
                .getAssetInfo(symbol(symbol))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found " + symbol));

        // Optional enrichment
        MarketAssetQuote quote = marketDataService.getCurrentQuote(symbol(symbol)).orElse(null);

        log.debug("Successfully retrieved asset info for {}: {}", symbol, assetInfo.getName());
        return ResponseEntity.ok(mapper.toAssetInfoResponse(assetInfo, quote));

    }

    /**
     * Get asset information for multiple symbols (batch operation).
     * 
     * Example: GET /api/market-data/asset-info?symbols=AAPL,GOOGL,BTC-USD
     */
    @GetMapping("/asset-info")
    @Operation(summary = "Get asset information for multiple symbols (batch)", description = "Retrieves detailed information for multiple assets in a single request")
    @ApiResponse(responseCode = "200", description = "Asset info retrieved successfully")
    public ResponseEntity<Map<String, AssetInfoResponse>> getBatchAssetInfo(@RequestParam List<String> symbols) {
        log.debug("Fetching batch asset info for {} symbols", symbols.size());
        List<SymbolIdentifier> symbolIds = symbols.stream()
                .map(this::symbol) // trim + uppercase
                .toList();

        Map<AssetIdentifier, MarketAssetInfo> assetInfoMap = marketDataService.getBatchAssetInfo(symbolIds);
        Map<AssetIdentifier, MarketAssetQuote> quoteMap = marketDataService.getBatchQuotes(symbolIds);
        Map<String, AssetInfoResponse> response =mapper.toAssetInfoResponseMap(assetInfoMap, quoteMap);

        log.info("Successfully retrieved asset info for {}/{} symbols",response.size(), symbols.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get the trading currency for an asset.
     * Useful for currency conversion calculations.
     * 
     * Example: GET /api/market-data/currency/AAPL -> "USD"
     */
    @GetMapping("/currency/{symbol}")
    @Operation(summary = "Get trading currency for a symbol", description = "Returns the currency in which the asset is traded (e.g., USD, CAD, EUR)")
    @ApiResponse(responseCode = "200", description = "Currency retrieved successfully")
    public ResponseEntity<String> getTradingCurrency(@PathVariable String symbol) {
        log.debug("Fetching trading currency for symbol: {}", symbol);

        SymbolIdentifier symbolId = symbol(symbol);
        String currency = marketDataService.getTradingCurrency(symbolId).getCode();

        log.debug("Trading currency for {}: {}", symbol, currency);
        return ResponseEntity.ok(currency);
    }

    /**
     * Check if a symbol is supported by the market data provider.
     * Use this to validate symbols before adding them to portfolios.
     * 
     * Example: GET /api/market-data/supported/AAPL -> true
     */
    @GetMapping("/supported/{symbol}")
    @Operation(summary = "Check if symbol is supported", description = "Validates whether the market data provider supports this symbol")
    @ApiResponse(responseCode = "200", description = "Check completed")
    public ResponseEntity<Boolean> isSymbolSupported(@PathVariable String symbol) {
        log.debug("Checking if symbol is supported: {}", symbol);
        boolean supported = marketDataService.isSymbolSupported(symbol(symbol));

        log.debug("Symbol {} supported: {}", symbol, supported);
        return ResponseEntity.ok(supported);
    }

    /**
     * Health check endpoint to verify market data service connectivity.
     * Returns basic connectivity status.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verify market data service is available and responsive")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean supported = marketDataService
                    .isSymbolSupported(SymbolIdentifier.of("AAPL"));

            return ResponseEntity.ok(Map.of(
                    "status", supported ? "UP" : "DEGRADED",
                    "service", "market-data",
                    "timestamp", Instant.now()));
        } catch (Exception e) {
            log.error("Market data health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "status", "DOWN",
                            "service", "market-data",
                            "error", e.getMessage(),
                            "timestamp", Instant.now()));
        }
    }

    private SymbolIdentifier symbol(String raw) {
        return SymbolIdentifier.of(raw.trim().toUpperCase());
    }
}
