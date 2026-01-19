package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;

@Profile({ "local", "test" })
@Service
public class CacheEvictionService {
    private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);

    /**
     * Cache eviction methods (for admin/testing purposes).
     * These allow manual cache clearing when needed.
     */


    @CacheEvict(value = "current-prices", key = "#symbol.value()")
    public void evictPriceCache(AssetIdentifier symbol) {
        log.info("Evicted price cache for symbol: {}", symbol.getPrimaryId());
    }

    @CacheEvict(value = "current-prices", allEntries = true)
    public void evictAllPriceCache() {
        log.info("Evicted all price caches");
    }

    @CacheEvict(value = "asset-info", key = "#symbol.value()")
    public void evictAssetInfoCache(AssetIdentifier symbol) {
        log.info("Evicted asset info cache for symbol: {}", symbol.getPrimaryId());
    }

    @CacheEvict(value = { "current-prices", "historical-prices", "asset-info", "trading-currency" }, allEntries = true)
    public void evictAllCaches() {
        log.info("Evicted all market data caches");
    }
}
