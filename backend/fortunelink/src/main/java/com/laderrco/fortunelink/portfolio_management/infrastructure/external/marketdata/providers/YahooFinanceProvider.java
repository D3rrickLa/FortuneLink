package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.providers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.mappers.YahooResponseMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.YahooQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.YahooQuoteResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.chart.YahooChartResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.chart.YahooChartResult;

import lombok.AllArgsConstructor;

/**
 * Yahoo Finance implementation of MarketDataProvider.
 * 
 * Uses Yahoo Finance's public JSON API endpoints:
 * - Quote: https://query1.finance.yahoo.com/v7/finance/quote?symbols=AAPL
 * - Chart: https://query1.finance.yahoo.com/v8/finance/chart/AAPL
 * 
 * This is an UNOFFICIAL API - Yahoo can change it anytime.
 * For production, consider:
 * 1. Adding rate limiting
 * 2. Implementing circuit breaker (Resilience4j)
 * 3. Having fallback providers
 */
@Component
@AllArgsConstructor
public class YahooFinanceProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceProvider.class);
    private static final String QUOTE_API = "https://query1.finance.yahoo.com/v6/finance/quote";
    private static final String CHART_API = "https://query1.finance.yahoo.com/v8/finance/chart";
    
    private final RestTemplate restTemplate;
    private final YahooResponseMapper mapper;
    
    @Override
    public Optional<ProviderQuote> fetchCurrentQuote(String symbol) {
        try {
            String url = Objects.requireNonNull(String.format("%s?symbols=%s", QUOTE_API, symbol), "URL IS NULL");
            YahooQuoteResponse response = Objects.requireNonNull(restTemplate.getForObject(url, YahooQuoteResponse.class), "Response is null");
            
            if (!hasValidQuoteResponse(response)) {
                log.warn("No valid quote data for symbol: {}", symbol);
                return Optional.empty();
            }
            
            YahooQuote quote = response.quoteResponse().result().get(0);
            return Optional.of(mapper.toProviderQuote(quote));
            
        } catch (RestClientException e) {
            log.error("Error fetching quote for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<ProviderQuote> fetchHistoricalQuote(String symbol, LocalDateTime dateTime) {
        try {
            long epochTime = dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
            long period1 = epochTime - 86400; // 24h before
            long period2 = epochTime + 86400; // 24h after
            
            String url = Objects.requireNonNull(String.format("%s/%s?period1=%d&period2=%d&interval=1d",
                CHART_API, symbol, period1, period2), "URL is null");
            
            YahooChartResponse response = Objects.requireNonNull(restTemplate.getForObject(url, YahooChartResponse.class), "Response is null");
            
            if (!hasValidChartResponse(response)) {
                log.warn("No historical data for {} at {}", symbol, dateTime);
                return Optional.empty();
            }
            
            YahooChartResult chartResult = response.chart().result().get(0);
            return mapper.extractClosestPrice(chartResult, dateTime, symbol);
            
        } catch (RestClientException e) {
            log.error("Error fetching historical quote for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, ProviderQuote> fetchBatchQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            String symbolsParam = String.join(",", symbols);
            String url = Objects.requireNonNull(String.format("%s?symbols=%s", QUOTE_API, symbolsParam), "URL is null");
            
            YahooQuoteResponse response = Objects.requireNonNull(restTemplate.getForObject(url, YahooQuoteResponse.class), "Response is null");
            
            if (!hasValidQuoteResponse(response)) {
                log.warn("No valid batch quote response");
                return Collections.emptyMap();
            }
            
            return response.quoteResponse().result().stream()
                .map(mapper::toProviderQuote)
                .collect(Collectors.toMap(ProviderQuote::symbol, q -> q));
                
        } catch (RestClientException e) {
            log.error("Error fetching batch quotes: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public Optional<ProviderAssetInfo> fetchAssetInfo(String symbol) {
        try {
            String url = Objects.requireNonNull(String.format("%s?symbols=%s", QUOTE_API, symbol), "URL is null");
            YahooQuoteResponse response = Objects.requireNonNull(restTemplate.getForObject(url, YahooQuoteResponse.class), "Response is null");
            
            if (!hasValidQuoteResponse(response)) {
                log.warn("No asset info for symbol: {}", symbol);
                return Optional.empty();
            }
            
            YahooQuote quote = response.quoteResponse().result().get(0);
            return Optional.of(mapper.toProviderAssetInfo(quote));
            
        } catch (RestClientException e) {
            log.error("Error fetching asset info for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, ProviderAssetInfo> fetchBatchAssetInfo(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            String symbolsParam = String.join(",", symbols);
            String url = Objects.requireNonNull(String.format("%s?symbols=%s", QUOTE_API, symbolsParam), "RUL is null");
            
            log.debug("Fetching batch asset info for {} symbols from Yahoo", symbols.size());
            YahooQuoteResponse response = Objects.requireNonNull(restTemplate.getForObject(url, YahooQuoteResponse.class), "Response is null");
            
            if (!hasValidQuoteResponse(response)) {
                log.warn("No valid batch asset info response");
                return Collections.emptyMap();
            }
            
            // Map each quote to ProviderAssetInfo
            Map<String, ProviderAssetInfo> result = response.quoteResponse().result().stream()
                .map(mapper::toProviderAssetInfo)
                .collect(Collectors.toMap(
                    ProviderAssetInfo::symbol, 
                    info -> info,
                    (existing, replacement) -> existing  // Handle duplicate keys (keep first)
                ));
            
            log.debug("Successfully fetched asset info for {}/{} symbols", result.size(), symbols.size());
            return result;
            
        } catch (RestClientException e) {
            log.error("Error fetching batch asset info: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public boolean supportsSymbol(String symbol) {
        // Yahoo supports most symbols - basic validation
        return symbol != null && !symbol.isBlank() && symbol.matches("[A-Z0-9\\.\\-^]+");
    }

    @Override
    public String getProviderName() {
        return "YAHOO_FINANCE";
    }
    
    // --- Private Validation Methods --- //
    
    private boolean hasValidQuoteResponse(YahooQuoteResponse response) {
        return response != null 
            && response.quoteResponse() != null
            && response.quoteResponse().hasResults();
    }
    
    private boolean hasValidChartResponse(YahooChartResponse response) {
        return response != null
            && response.chart() != null
            && response.chart().hasResults();
    }
}
