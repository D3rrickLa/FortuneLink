package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;

/**
 * Strategy interface for market data providers.
 * Allows swapping between Yahoo, Alpha Vantage, Finnhub, etc.
 * 
 * This is part of the Anti-Corruption Layer (ACL).
 * Implementations should:
 * 1. Handle provider-specific API calls
 * 2. Map responses to internal models (ProviderQuote, ProviderAssetInfo)
 * 3. NOT throw domain exceptions - return empty/null for failures
 */
public interface MarketDataProvider {
    
    /**
     * Fetch current quote for a single symbol.
     * 
     * @param symbol Raw symbol (e.g., "AAPL", "BTC-USD")
     * @return Quote if available, empty if not found or error
     */
    Optional<ProviderQuote> fetchCurrentQuote(String symbol);
    
    /**
     * Fetch historical quote at specific date.
     * 
     * @param symbol Raw symbol
     * @param dateTime Point in time
     * @return Quote if available, empty if not found or error
     */
    Optional<ProviderQuote> fetchHistoricalQuote(String symbol, LocalDateTime dateTime);
    
    /**
     * Batch fetch current quotes (more efficient).
     * 
     * @param symbols List of raw symbols
     * @return Map of successful fetches (excludes failures)
     */
    Map<String, ProviderQuote> fetchBatchQuotes(List<String> symbols);
    
    /**
     * Fetch asset metadata.
     * 
     * @param symbol Raw symbol
     * @return Asset info if available
     */
    Optional<ProviderAssetInfo> fetchAssetInfo(String symbol);

    /**
     * Batch fetch asset info 
     * 
     * @param symbols list of raw symbols
     * @return Map of successful fetches (excludes failures)
     */
    Map<String, ProviderAssetInfo> fetchBatchAssetInfo(List<String> symbols);
    
    /**
     * Check if this provider supports the symbol.
     * 
     * @param symbol Raw symbol
     * @return true if supported
     */
    boolean supportsSymbol(String symbol);
    
    /**
     * Get provider name for logging/debugging.
     */
    String getProviderName();
}