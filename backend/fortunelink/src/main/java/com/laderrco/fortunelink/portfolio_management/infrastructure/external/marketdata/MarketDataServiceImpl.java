package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataProvider;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.MarketDataMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

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

    /**
     * Get current price with caching, TTL of 5 min
     */
    @Override
    @Cacheable(value = "current-prices", key = "#assetIdentifier.getPrimaryId()", unless = "#result == null")
    public Money getCurrentPrice(AssetIdentifier assetIdentifier) {
        log.debug("Fetching current price for symbol: {}", assetIdentifier.getPrimaryId());

        // String providerSymbol =
        // mapper.toProviderSymbol(assetIdentifier.getPrimaryId(),
        // provider.getProviderName());
        Optional<ProviderQuote> quote = provider.fetchCurrentQuote(assetIdentifier.getPrimaryId());

        if (quote.isEmpty()) {
            throw MarketDataException.symbolNotFound(assetIdentifier.getPrimaryId());
        }

        Money price = mapper.toMoney(quote.get());

        log.debug("Retrieved price for {}: {}", assetIdentifier.getPrimaryId(), price);
        return price;
    }

    /**
     * NOTE: this doesn't actually exists as in i didn't code the provider method
     * Get historical price with caching.
     * Cache key: "historical-prices::AAPL::2026-01-01T00:00:00"
     * TTL: 24 hours (historical data doesn't change)
     */
    @Override
    @Cacheable(value = "historical-prices", key = "#assetIdentifier.getPrimaryId() + '::' + #dateTime.toString()", unless = "#result == null")
    public Money getHistoricalPrice(AssetIdentifier assetIdentifier, LocalDateTime dateTime) {
        log.debug("Fetching historical price for {} at {}", assetIdentifier.getPrimaryId(), dateTime);

        // NOT NEEDED FOR MVP
        // String providerSymbol = mapper.toProviderSymbol(assetIdentifier,
        // provider.getProviderName());
        Optional<ProviderQuote> quote = provider.fetchHistoricalQuote(assetIdentifier.getPrimaryId(), dateTime);

        if (quote.isEmpty()) {
            throw MarketDataException.dataUnavailable(assetIdentifier.getPrimaryId(),
                    "No historical data available for " + dateTime);
        }

        Money price = mapper.toMoney(quote.get());
        log.debug("Retrieved historical price for {} at {}: {}", assetIdentifier.getPrimaryId(), dateTime, price);
        return price;
    }

    /**
     * Get batch prices.
     * Note: Batch operations are harder to cache effectively.
     * Each individual price is cached by getCurrentPrice() if called separately.
     * 
     * For true batch caching, we'd need a custom cache key generator.
     */
    @Override
    public Map<AssetIdentifier, Money> getBatchPrices(List<? extends AssetIdentifier> assetIdentifiers) {
        if (assetIdentifiers == null || assetIdentifiers.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Fetching batch prices for {} symbols", assetIdentifiers.size());

        List<String> providerSymbols = assetIdentifiers.stream()
                .map(s -> mapper.toProviderSymbol(s, provider.getProviderName()))
                .toList();

        Map<String, ProviderQuote> batchQuotes = provider.fetchBatchQuotes(providerSymbols);

        Map<AssetIdentifier, Money> result = new HashMap<>();

        for (AssetIdentifier symbol : assetIdentifiers) {
            try {
                // Reuse cached / rate-limited method
                Money price = resolvePrice(symbol, batchQuotes);
                result.put(symbol, price);
            } catch (MarketDataException e) {
                log.warn("Failed to fetch price for {}: {}", symbol.getPrimaryId(), e.getMessage());
            }
        }

        log.debug("Batch fetch complete: {}/{} symbols retrieved", result.size(), assetIdentifiers.size());
        return result;
    }

    /**
     * Get asset info with caching.
     * Cache key: "asset-info::AAPL"
     * TTL: 7 days (metadata rarely changes)
     */
    @Override
    @Cacheable(value = "asset-info", key = "#identifier.getPrimaryId()", unless = "#result == null")
    public Optional<MarketAssetInfo> getAssetInfo(AssetIdentifier identifier) {
        log.debug("Fetching asset info for symbol: {}", identifier.getPrimaryId());

        // String providerSymbol = mapper.toProviderSymbol(identifier,
        // provider.getProviderName());
        Optional<ProviderAssetInfo> providerInfo = provider.fetchAssetInfo(identifier.getPrimaryId());

        if (providerInfo.isEmpty()) {
            log.debug("No asset info found for {}", identifier.getPrimaryId());
            return Optional.empty();
        }

        MarketAssetInfo info = mapper.toAssetInfo(providerInfo.get());
        log.debug("Retrieved asset info for {}: {}", identifier.getPrimaryId(), info.getName());
        return Optional.of(info);
    }

    /**
     * Get batch asset info.
     * Similar strategy to getBatchPrices - leverages individual caching.
     */
    @Override
    public Map<AssetIdentifier, MarketAssetInfo> getBatchAssetInfo(List<? extends AssetIdentifier> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Fetching batch asset info for {} symbols", symbols.size());

        List<String> providerSymbols = symbols.stream()
                .map(s -> mapper.toProviderSymbol(s, provider.getProviderName()))
                .toList();

        Map<String, ProviderAssetInfo> providerInfoMap = provider.fetchBatchAssetInfo(providerSymbols);

        Map<AssetIdentifier, MarketAssetInfo> result = symbols.stream()
                .map(symbol -> {
                    ProviderAssetInfo providerInfo = providerInfoMap.get(symbol.getPrimaryId());
                    if (providerInfo == null) {
                        log.warn("No asset info found for symbol: {}", symbol.getPrimaryId());
                        return null;
                    }
                    return Map.entry(symbol, mapper.toAssetInfo(providerInfo));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.debug("Retrieved asset info for {}/{} symbols", result.size(), symbols.size());
        return result;
    }

    @Override
    public boolean isSymbolSupported(AssetIdentifier symbol) {
        // String providerSymbol = mapper.toProviderSymbol(symbol,
        // provider.getProviderName());
        return provider.supportsSymbol(symbol.getPrimaryId());
    }

    /**
     * Get trading currency with caching.
     * Cache key: "trading-currency::AAPL"
     * TTL: 7 days (trading currency rarely changes)
     */
    @Override
    @Cacheable(value = "trading-currency", key = "#symbol.value()", unless = "#result == null")
    public ValidatedCurrency getTradingCurrency(AssetIdentifier assetIdentifier) {
        log.debug("Cache miss for trading currency: {} (fetching from provider)", assetIdentifier.getPrimaryId());

        // Delegate to getAssetInfo (avoids duplicate API call logic)
        MarketAssetInfo info = getAssetInfo(assetIdentifier)
                .orElseThrow(() -> MarketDataException.symbolNotFound(assetIdentifier.getPrimaryId()));
        ValidatedCurrency currency = info.getCurrency();

        log.debug("Fetched and cached trading currency for {}: {}", assetIdentifier.getPrimaryId(), currency);
        return currency;
    }

    /**
     * Resolves a single AssetIdentifier to Money.
     * Checks batchQuotes first, falls back to getCurrentPrice().
     */
    private Money resolvePrice(AssetIdentifier asset, Map<String, ProviderQuote> batchQuotes) {
        String symbol = asset.getPrimaryId();
        if (batchQuotes.containsKey(symbol)) {
            return mapper.toMoney(batchQuotes.get(symbol));
        } else {
            // Fallback to single fetch (current implementation)
            return getCurrentPrice(asset);
        }
    }

}
