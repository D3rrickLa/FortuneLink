package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.FmpConfigurationProperties;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.FmpApiException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpQuoteResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Low-level HTTP client for FMP API.
 * 
 * Responsibilities:
 * - Construct FMP API URLs
 * - Execute HTTP requests
 * - Parse JSON responses
 * - Handle HTTP errors
 * 
 * Does NOT contain business logic - just HTTP communication.
 */
@Slf4j
@Component
public class FmpApiClient {

    private final FmpConfigurationProperties config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Initialize HTTP client bean.
     */
    public FmpApiClient(FmpConfigurationProperties config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        
        // Validate config on startup
        config.validate();
        log.info("FMP API Client initialized with base URL: {}", config.getBaseUrl());
    }

    /**
     * Get real-time quote for a single symbol.
     * 
     * Endpoint: GET /quote/{symbol}?apikey={key}
     * Returns: List with 1 element (FMP returns array even for single symbol)
     */
    public FmpQuoteResponse getQuote(String symbol) {
        String url = buildUrl("/quote/" + encodeSymbol(symbol));
        
        if (config.isDebugLogging()) {
            log.debug("FMP Request: GET {}", url);
        }
        
        try {
            String jsonResponse = executeGetRequest(url);
            
            if (config.isDebugLogging()) {
                log.debug("FMP Response: {}", jsonResponse);
            }
            
            // FMP returns array even for single symbol
            List<FmpQuoteResponse> quotes = objectMapper.readValue(
                jsonResponse, 
                new TypeReference<List<FmpQuoteResponse>>() {}
            );
            
            if (quotes.isEmpty()) {
                throw new FmpApiException("Symbol not found: " + symbol);
            }
            
            return quotes.get(0);
            
        } catch (IOException e) {
            throw new FmpApiException("Failed to parse FMP quote response for " + symbol, e);
        }
    }

    /**
     * Get real-time quotes for multiple symbols (batch).
     * 
     * Endpoint: GET /quote/{symbol1,symbol2,symbol3}?apikey={key}
     * More efficient than individual calls (saves API quota).
     */
    public List<FmpQuoteResponse> getBatchQuotes(List<String> symbols) {
        if (symbols.isEmpty()) {
            return List.of();
        }
        
        String symbolsParam = String.join(",", symbols);
        String url = buildUrl("/quote?symbol=" + encodeSymbol(symbolsParam));
        
        if (config.isDebugLogging()) {
            log.debug("FMP Batch Request: GET {} (symbols: {})", url, symbols.size());
        }
        
        try {
            String jsonResponse = executeGetRequest(url);
            
            List<FmpQuoteResponse> quotes = objectMapper.readValue(
                jsonResponse,
                new TypeReference<List<FmpQuoteResponse>>() {}
            );
            
            log.info("Retrieved {} quotes from FMP (requested {})", quotes.size(), symbols.size());
            
            return quotes;
            
        } catch (IOException e) {
            throw new FmpApiException("Failed to parse FMP batch quotes response", e);
        }
    }

    /**
     * Get company/asset profile (detailed metadata).
     * 
     * Endpoint: GET /profile/{symbol}?apikey={key}
     */
    public FmpProfileResponse getProfile(String symbol) {
        String url = buildUrl("/profile?symbol=" + encodeSymbol(symbol));
        
        if (config.isDebugLogging()) {
            log.debug("FMP Profile Request: GET {}", url);
        }
        
        try {
            String jsonResponse = executeGetRequest(url);
            
            // FMP returns array even for single symbol
            List<FmpProfileResponse> profiles = objectMapper.readValue(
                jsonResponse,
                new TypeReference<List<FmpProfileResponse>>() {}
            );
            
            if (profiles.isEmpty()) {
                throw new FmpApiException("Profile not found for symbol: " + symbol);
            }
            
            return profiles.get(0);
            
        } catch (IOException e) {
            throw new FmpApiException("Failed to parse FMP profile response for " + symbol, e);
        }
    }

    /**
     * Get profiles for multiple symbols (batch).
     * 
     * Note: FMP doesn't have a batch profile endpoint, so we make individual calls.
     * For large batches, consider caching or rate limiting.
     */
    public List<FmpProfileResponse> getBatchProfiles(List<String> symbols) {
        log.warn("Batch profile requests make {} individual API calls. Consider caching.", symbols.size());
        
        return symbols.stream()
                .map(this::getProfile)
                .toList();
    }

    /**
     * Execute HTTP GET request and return response body as string.
     */
    private String executeGetRequest(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check HTTP status
            if (response.statusCode() == 200) {
                return response.body();
            } else if (response.statusCode() == 401) {
                throw new FmpApiException("Invalid FMP API key. Check your configuration.");
            } else if (response.statusCode() == 429) {
                throw new FmpApiException("FMP API rate limit exceeded. Free tier: 250 requests/day.");
            } else if (response.statusCode() == 404) {
                throw new FmpApiException("FMP API endpoint not found: " + url);
            } else {
                throw new FmpApiException(
                    String.format("FMP API error: HTTP %d - %s", response.statusCode(), response.body())
                );
            }

        } catch (IOException e) {
            throw new FmpApiException("Network error calling FMP API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FmpApiException("Request interrupted", e);
        }
    }

    /**
     * Build full API URL with base URL and API key.
     */
    private String buildUrl(String endpoint) {
        log.debug("FMP CLIENT DEBUG URL: "+config.getBaseUrl());
        return String.format("%s%s&apikey=%s", config.getBaseUrl(), endpoint, config.getApiKey());
    }

    /**
     * URL-encode symbol (handles special chars like BTC-USD).
     */
    private String encodeSymbol(String symbol) {
        return URLEncoder.encode(symbol, StandardCharsets.UTF_8);
    }
}