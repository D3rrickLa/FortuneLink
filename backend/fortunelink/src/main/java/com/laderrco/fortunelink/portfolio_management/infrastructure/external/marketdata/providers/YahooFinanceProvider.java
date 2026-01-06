package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.providers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;

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
public class YahooFinanceProvider implements MarketDataProvider {

    @Override
    public Optional<ProviderQuote> fetchCurrentQuote(String symbol) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fetchCurrentQuote'");
    }

    @Override
    public Optional<ProviderQuote> fetchHistoricalQuote(String symbol, LocalDateTime dateTime) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fetchHistoricalQuote'");
    }

    @Override
    public Map<String, ProviderQuote> fetchBatchQuotes(List<String> symbols) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fetchBatchQuotes'");
    }

    @Override
    public Optional<ProviderAssetInfo> fetchAssetInfo(String symbol) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fetchAssetInfo'");
    }

    @Override
    public boolean supportsSymbol(String symbol) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'supportsSymbol'");
    }

    @Override
    public String getProviderName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProviderName'");
    }
    
}
