package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.mappers.MarketDataMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.providers.MarketDataProvider;
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
 */
@Service
@AllArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {
    private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);

    private final MarketDataProvider provider;
    private final MarketDataMapper mapper;

    @Override
    public Money getCurrentPrice(AssetIdentifier assetIdentifier) {
        log.debug("Fetching current price for symbol: {}", assetIdentifier.getPrimaryId());

        // CHECKS cache first (TODO: impelemnt Redis caching)
        // Money cached = checkCache(symbol);
        // if (cached != null) return cached;

        String providerSymbol = mapper.toProviderSymbol(assetIdentifier, provider.getProviderName());

        Optional<ProviderQuote> quote = provider.fetchCurrentQuote(providerSymbol);

        if (quote.isEmpty()) {
            throw MarketDataException.symbolNotFound(assetIdentifier.getPrimaryId());
        }

        Money price = mapper.toMoney(quote.get());

        // Cach result -> TODO: Implement
        // cachePrice(symbol, price);

        log.debug("Retrieved price for {}: {}", assetIdentifier.getPrimaryId(), price);
        return price;
    }

    @Override
    public Money getHistoricalPrice(AssetIdentifier assetIdentifierId, LocalDateTime dateTime) {
        log.debug("Fetching historical price for {} at {}", assetIdentifierId.getPrimaryId(), dateTime);

        String providerSymbol = mapper.toProviderSymbol(assetIdentifierId, provider.getProviderName());

        Optional<ProviderQuote> quote = provider.fetchHistoricalQuote(providerSymbol, dateTime);

        if (quote.isEmpty()) {
            throw MarketDataException.dataUnavailable(
                    assetIdentifierId.getPrimaryId(),
                    "No historical data available for " + dateTime);
        }

        Money price = mapper.toMoney(quote.get());
        log.debug("Retrieved historical price for {} at {}: {}", assetIdentifierId.getPrimaryId(), dateTime, price);
        return price;
    }

    @Override
    public Map<AssetIdentifier, Money> getBatchPrices(List<AssetIdentifier> assetIdentifiers) {
        if (assetIdentifiers == null || assetIdentifiers.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Fetching batch prices for {} symbols", assetIdentifiers.size());

        // convert domain symbols to provider format
        List<String> providerSymbols = assetIdentifiers.stream()
                .map(s -> mapper.toProviderSymbol(s, provider.getProviderName()))
                .toList();
        // Fetch from provider
        Map<String, ProviderQuote> quotes = provider.fetchBatchQuotes(providerSymbols);

        // Map back to domain
        Map<AssetIdentifier, Money> result = new HashMap<>();
        for (AssetIdentifier symbol : assetIdentifiers) {
            String providerSymbol = mapper.toProviderSymbol(symbol, provider.getProviderName());
            ProviderQuote quote = quotes.get(providerSymbol);

            if (quote != null) {
                result.put(symbol, mapper.toMoney(quote));
            } else {
                log.warn("No price data for symbol: {}", symbol.getPrimaryId());
            }
        }

        log.debug("Retrieved prices for {}/{} symbols", result.size(), assetIdentifiers.size());
        return result;
    }

    @Override
    public Optional<MarketAssetInfo> getAssetInfo(String symbol) {
        log.debug("Fetching asset info for symbol: {}", symbol);

        String providerSymbol = mapper.toProviderSymbol(symbol, provider.getProviderName());

        Optional<ProviderAssetInfo> providerInfo = provider.fetchAssetInfo(providerSymbol);

        if (providerInfo.isEmpty()) {
            log.debug("No asset info found for {}", symbol);
            return Optional.empty();
        }

        MarketAssetInfo info = mapper.toAssetInfo(providerInfo.get());
        log.debug("Retrieved asset info for {}: {}", symbol, info.getName());
        return Optional.of(info);
    }

    @Override
    public Map<AssetIdentifier, MarketAssetInfo> getAssetInfoBatch(List<AssetIdentifier> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Fetching batch asset info for {} symbols", symbols.size());

        // Convert domain symbols to provider format
        List<String> providerSymbols = symbols.stream()
                .map(s -> mapper.toProviderSymbol(s.getPrimaryId(), provider.getProviderName()))
                .toList();

        // ✅ USE THE BATCH ASSET INFO METHOD - single API call!
        Map<String, ProviderAssetInfo> providerInfoMap = provider.fetchBatchAssetInfo(providerSymbols);

        // Map back to domain models
        Map<AssetIdentifier, MarketAssetInfo> result = new HashMap<>();
        for (AssetIdentifier symbol : symbols) {
            String providerSymbol = mapper.toProviderSymbol(symbol.getPrimaryId(), provider.getProviderName());
            ProviderAssetInfo providerInfo = providerInfoMap.get(providerSymbol);

            if (providerInfo != null) {
                result.put(symbol, mapper.toAssetInfo(providerInfo));
            } else {
                log.warn("No asset info found for symbol: {}", symbol.getPrimaryId());
            }
        }

        log.debug("Retrieved asset info for {}/{} symbols", result.size(), symbols.size());
        return result;
    }

    @Override
    public boolean isSymbolSupported(AssetIdentifier symbol) {
        String providerSymbol = mapper.toProviderSymbol(symbol, provider.getProviderName());
        return provider.supportSymbol(providerSymbol);
    }

    @Override
    public ValidatedCurrency getTradingCurrency(AssetIdentifier assetIdentifier) {
        // Delegate to getAssetInfo (avoids duplicate API call logic)
        MarketAssetInfo info = getAssetInfo(assetIdentifier.getPrimaryId())
                .orElseThrow(() -> MarketDataException.symbolNotFound(assetIdentifier.getPrimaryId()));

        return info.getCurrency();
    }


    // --- Caching Methods (TODO: Implement with Redis) ---
    
    /*
    private Money checkCache(AssetSymbol symbol) {
        // Check Redis cache for recent price
        // Key format: "market:price:{symbol}:{currency}"
        // TTL: 5 minutes for current prices, 24h for historical
        return null;
    }
    
    private void cachePrice(AssetSymbol symbol, Money price) {
        // Store in Redis with TTL
        // cacheManager.getCache("market-data").put(symbol.value(), price);
    }
    */

}
