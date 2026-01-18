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
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
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

        // String providerSymbol = mapper.toProviderSymbol(assetIdentifier.getPrimaryId(), provider.getProviderName());

        Optional<ProviderQuote> quote = provider.fetchCurrentQuote(assetIdentifier.getPrimaryId());

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
    public Money getHistoricalPrice(AssetIdentifier assetIdentifier, LocalDateTime dateTime) {
        log.debug("Fetching historical price for {} at {}", assetIdentifier.getPrimaryId(), dateTime);

        // NOT NEEDED FOR MVP
        // String providerSymbol = mapper.toProviderSymbol(assetIdentifier, provider.getProviderName());

        Optional<ProviderQuote> quote = provider.fetchHistoricalQuote(assetIdentifier.getPrimaryId(), dateTime);

        if (quote.isEmpty()) {
            throw MarketDataException.dataUnavailable(assetIdentifier.getPrimaryId(), "No historical data available for " + dateTime);
        }

        Money price = mapper.toMoney(quote.get());
        log.debug("Retrieved historical price for {} at {}: {}", assetIdentifier.getPrimaryId(), dateTime, price);
        return price;
    }

    @Override
    public Map<AssetIdentifier, Money> getBatchPrices(List<? extends AssetIdentifier> assetIdentifiers) {
        if (assetIdentifiers == null || assetIdentifiers.isEmpty()) { return Collections.emptyMap(); }

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
            // String providerSymbol = mapper.toProviderSymbol(symbol, provider.getProviderName());
            ProviderQuote quote = quotes.get(symbol.getPrimaryId());

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
    public Optional<MarketAssetInfo> getAssetInfo(AssetIdentifier identifier) {
        log.debug("Fetching asset info for symbol: {}", identifier.getPrimaryId());

        // String providerSymbol = mapper.toProviderSymbol(identifier, provider.getProviderName());

        Optional<ProviderAssetInfo> providerInfo = provider.fetchAssetInfo(identifier.getPrimaryId());

        if (providerInfo.isEmpty()) {
            log.debug("No asset info found for {}", identifier.getPrimaryId());
            return Optional.empty();
        }

        MarketAssetInfo info = mapper.toAssetInfo(providerInfo.get());
        log.debug("Retrieved asset info for {}: {}", identifier.getPrimaryId(), info.getName());
        return Optional.of(info);
    }

    @Override
    public Optional<MarketAssetInfo> getAssetInfo(String symbol) {
        // Right now the this is being used only in the PortfolioApplicationService 
        // specifically the record buy, sell, and divivdend methods. will build 
        // a marktet identifier

        AssetIdentifier marketIdentifier = new MarketIdentifier(
            symbol,
            null,
            AssetType.STOCK, // because we don't know if this is a stock or etf, default to stock
            "GetAssetInfoItem",
            "UNKNOWN UOT",
            null
        );
        return getAssetInfo(marketIdentifier);
    }

    @Override
    public Map<AssetIdentifier, MarketAssetInfo> getBatchAssetInfo(List<? extends AssetIdentifier> symbols) {
        if (symbols == null || symbols.isEmpty()) { return Collections.emptyMap(); }

        log.debug("Fetching batch asset info for {} symbols", symbols.size());

        // Convert domain symbols to provider format
        List<String> providerSymbols = symbols.stream()
                .map(s -> mapper.toProviderSymbol(s, provider.getProviderName()))
                .toList();

        Map<String, ProviderAssetInfo> providerInfoMap = provider.fetchBatchAssetInfo(providerSymbols);

        // Map back to domain models
        Map<AssetIdentifier, MarketAssetInfo> result = new HashMap<>();
        for (AssetIdentifier symbol : symbols) {
            // String providerSymbol = mapper.toProviderSymbol(symbol, provider.getProviderName());
            ProviderAssetInfo providerInfo = providerInfoMap.get(symbol.getPrimaryId());

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
        // String providerSymbol = mapper.toProviderSymbol(symbol, provider.getProviderName());
        return provider.supportsSymbol(symbol.getPrimaryId());
    }

    @Override
    public ValidatedCurrency getTradingCurrency(AssetIdentifier assetIdentifier) {
        // Delegate to getAssetInfo (avoids duplicate API call logic)
        MarketAssetInfo info = getAssetInfo(assetIdentifier)
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
