package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataProvider;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.MarketDataMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import lombok.AllArgsConstructor;

/**
 * Primary implementation of MarketDataService.
 * 
 * Architecture:
 * 1. Delegates to pluggable MarketDataProvider (Yahoo, Alpha Vantage, etc.)
 * 2. Uses MarketDataMapper for ACL translation
 * 3. Handles domain exceptions and error mapping
 * 4. Provides caching (commented out for MVP - add Redis later)
 * 
 * Thread-safe: All operations are stateless or use thread-safe components.
 * 
 * Cache Strategy:
 * - current-prices: 5 min TTL (prices change frequently)
 * - historical-prices: 24 hours TTL (historical data is immutable)
 * - asset-info: 7 days TTL (metadata rarely changes)
 * - trading-currency: 7 days TTL (currency rarely changes)
 * 
 * Cache Keys:
 * - Uses SpEL (Spring Expression Language) for dynamic keys
 * - Format: "cacheName::keyExpression"
 * - Example: "current-prices::AAPL"
 */
@Service
@AllArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {
    private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);

    private final MarketDataProvider provider;
    private final MarketDataMapper mapper;

    @Override
    @Cacheable(value = "current-prices", key = "#identifier.cacheKey()", unless = "#result == null")
    public Optional<MarketAssetQuote> getCurrentQuote(AssetIdentifier identifier) {
        log.debug("Fetching current price for symbol: {}", identifier.getPrimaryId());
        String providerSymbol = mapper.toProviderSymbol(identifier, provider.getProviderName());

        Optional<ProviderQuote> providerQuote = provider.fetchCurrentQuote(providerSymbol);

        if (providerQuote.isEmpty()) {
            log.debug("No quote found for provider symbol {}", providerSymbol);
            return Optional.empty();
        }

        return Optional.of(mapper.toAssetQuote(identifier, providerQuote.get()));

    }

    @Override
    @Cacheable(value = "historical-prices", key = "#identifier.cacheKey() + '::' + #date.toString()", unless = "#result == null")
    public Optional<MarketAssetQuote> getHistoricalQuote(AssetIdentifier identifier, LocalDateTime date) {
        log.debug("Fetching historical price for {} at {} ", identifier.getPrimaryId(), date);
        String providerSymbol = mapper.toProviderSymbol(identifier, provider.getProviderName());

        Optional<ProviderQuote> quote = provider.fetchHistoricalQuote(providerSymbol, date);
        if (quote.isEmpty()) {
            log.debug("No historical quote found for symbol {}", providerSymbol);
            return Optional.empty();
        }

        return Optional.of(mapper.toAssetQuote(identifier, quote.get()));
    }

    @Override
    public Map<AssetIdentifier, MarketAssetQuote> getBatchQuotes(
            List<? extends AssetIdentifier> identifiers) {

        if (identifiers == null || identifiers.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Fetching batch prices for {} symbols", identifiers.size());

        Map<String, AssetIdentifier> providerToIdentifier = new HashMap<>();
        List<String> providerSymbols = new ArrayList<>();

        for (AssetIdentifier id : identifiers) {
            String providerSymbol = mapper.toProviderSymbol(id, provider.getProviderName());
            providerSymbols.add(providerSymbol);
            providerToIdentifier.put(providerSymbol, id);
        }

        Map<String, ProviderQuote> batchQuotes = provider.fetchBatchQuotes(providerSymbols);

        Map<AssetIdentifier, MarketAssetQuote> result = new HashMap<>();

        for (Map.Entry<String, ProviderQuote> entry : batchQuotes.entrySet()) {
            AssetIdentifier id = providerToIdentifier.get(entry.getKey());
            if (id == null)
                continue;

            try {
                result.put(id, mapper.toAssetQuote(id, entry.getValue()));
            } catch (Exception e) {
                log.warn("Failed to map quote for {}: {}", entry.getKey(), e.getMessage());
            }
        }

        log.debug("Batch fetch complete: {}/{} symbols retrieved",
                result.size(), identifiers.size());

        return result;
    }

    @Override
    @Cacheable(value = "asset-info", key = "#identifier.cacheKey()", unless = "#result == null")
    public Optional<MarketAssetInfo> getAssetInfo(AssetIdentifier identifier) {
        log.debug("Fetching asset info for {}", identifier.getPrimaryId());

        String providerSymbol = mapper.toProviderSymbol(identifier, provider.getProviderName());

        Optional<ProviderAssetInfo> providerInfo = provider.fetchAssetInfo(providerSymbol);

        if (providerInfo.isEmpty()) {
            log.debug("No asset info found for {}", providerSymbol);
            return Optional.empty();
        }

        return Optional.of(mapper.toAssetInfo(providerInfo.get()));
    }

    @Override
    public Map<AssetIdentifier, MarketAssetInfo> getBatchAssetInfo(List<? extends AssetIdentifier> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, AssetIdentifier> providerToIdentifier = new HashMap<>();
        List<String> providerSymbols = new ArrayList<>();

        for (AssetIdentifier id : identifiers) {
            String providerSymbol = mapper.toProviderSymbol(id, provider.getProviderName());
            providerSymbols.add(providerSymbol);
            providerToIdentifier.put(providerSymbol, id);
        }

        Map<String, ProviderAssetInfo> providerResults = provider.fetchBatchAssetInfo(providerSymbols);

        Map<AssetIdentifier, MarketAssetInfo> result = new HashMap<>();

        for (Map.Entry<String, ProviderAssetInfo> entry : providerResults.entrySet()) {
            AssetIdentifier id = providerToIdentifier.get(entry.getKey());
            if (id == null)
                continue;

            result.put(id, mapper.toAssetInfo(entry.getValue()));
        }

        return result;
    }

    @Override
    @Cacheable(value = "trading-currency", key = "#assetIdentifier.cacheKey()", unless = "#result == null")
    public ValidatedCurrency getTradingCurrency(AssetIdentifier assetIdentifier) {
        return getAssetInfo(assetIdentifier)
                .map(MarketAssetInfo::getCurrency)
                .orElseThrow(() -> MarketDataException.symbolNotFound(
                        assetIdentifier.getPrimaryId()));
    }

    @Override
    public boolean isSymbolSupported(AssetIdentifier symbol) {
        String providerSymbol = mapper.toProviderSymbol(symbol, provider.getProviderName());
        return provider.supportsSymbol(providerSymbol);
    }
}
