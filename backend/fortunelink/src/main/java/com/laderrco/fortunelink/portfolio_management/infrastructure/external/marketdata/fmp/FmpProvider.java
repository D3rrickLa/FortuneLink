package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataProvider;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpQuoteResponse;

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
    private final FmpResponseMapper mapper;

    @Override
    public Optional<ProviderQuote> fetchCurrentQuote(String symbol) {
        try {
            FmpQuoteResponse response = fmpApiClient.getQuote(symbol);
            return Optional.ofNullable(response).map(mapper::toProviderQuote);
        } catch (Exception e) {
            log.error("Failed to fetch current quote for symbol: {} from FMP", symbol, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ProviderQuote> fetchHistoricalQuote(String symbol, LocalDateTime dateTime) {
        // NOTE: This is gatekept in their sub models so no IMP
        throw new UnsupportedOperationException("Unimplemented method 'fetchHistoricalQuote'");
    }

    @Override
    public Map<String, ProviderQuote> fetchBatchQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) { return Collections.emptyMap(); }

        try {
            List<FmpQuoteResponse> responses = fmpApiClient.getBatchQuotes(symbols);
            return responses.stream()
                .collect(Collectors.toMap(
                    FmpQuoteResponse::getSymbol,
                    mapper::toProviderQuote,
                    (existing, replacement) -> existing
                ));
        } catch (Exception e) {
            log.error("Critical failure during FMP batch quote fetch for {} symbols", symbols.size(), e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Optional<ProviderAssetInfo> fetchAssetInfo(String symbol) {
        try {
            FmpProfileResponse response = fmpApiClient.getProfile(symbol);
            return Optional.ofNullable(response).map(mapper::toProviderAssetInfo);
        } catch (Exception e) {
            log.error("Failed to fetch asset info for symbol: {} from FMP", symbol, e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, ProviderAssetInfo> fetchBatchAssetInfo(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) { return Collections.emptyMap(); }

        // Note: BatchProfiles usually performs individual calls. 
        // We use a stream here to handle individual failures so one 404 doesn't kill the batch.
        // NOTE: FMP does have a 'batch' quote option BUT it's gatekept by their sub so not using it
        // else i would as that saves API request
        return symbols.stream()
            .map(s -> {
                try { return Optional.of(fmpApiClient.getProfile(s)); }
                catch (Exception e) {
                    log.warn("Skipping asset info for {} due to provider error", s);
                    return Optional.<FmpProfileResponse>empty();
                }
            })
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(
                FmpProfileResponse::getSymbol,
                mapper::toProviderAssetInfo,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public boolean supportsSymbol(String symbol) {
        return symbol != null && !symbol.isBlank() && symbol.matches("[A-Z0-9\\.\\-^]+");
    }

    @Override
    public String getProviderName() {
        return "FMP";
    }
}
