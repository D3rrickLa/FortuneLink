package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.providers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.api_clients.FmpApiClient;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FMP (Financial Modeling Prep) implementation of MarketDataService.
 * 
 * This is the adapter between our domain (MarketDataService interface)
 * and the external FMP API.
 * 
 * Key Features:
 * - Real-time stock prices (US exchanges)
 * - Crypto prices (BTC-USD, ETH-USD, etc.)
 * - ETF prices
 * - Company profiles and metadata
 * - Batch operations for efficiency
 * 
 * Free Tier Limits:
 * - 250 API calls per day
 * - Real-time data with 15-minute delay
 * 
 * @Primary annotation makes this the default MarketDataService implementation
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class FmpProvider implements MarketDataProvider {

    private final FmpApiClient fmpApiClient;

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
    public Map<String, ProviderAssetInfo> fetchBatchAssetInfo(List<String> symbols) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fetchBatchAssetInfo'");
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
