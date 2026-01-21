package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;


@SpringBootTest(classes = {
        CacheEvictionService.class,
        CacheEvictionServiceTest.TestCacheConfig.class
})
@ActiveProfiles("test")
class CacheEvictionServiceTest {

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Autowired
    private CacheManager cacheManager;

    private MarketIdentifier symbol;

    @SuppressWarnings("null")
    @BeforeEach
    void setUp() {
        symbol = new MarketIdentifier(
                "AAPL",
                null,
                AssetType.STOCK,
                "Apple Inc.",
                "USD",
                Map.of(
                        "price", BigDecimal.TEN.toString(),
                        "info", "apple the company"
                )
        );

        // 🔑 IMPORTANT: keys must match @CacheEvict key
        cache("current-prices").put(symbol.getPrimaryId(), "price");
        cache("asset-info").put(symbol.getPrimaryId(), "info");
        cache("historical-prices").put("ANY", "history");
        cache("trading-currency").put("USD", "currency");
    }

    @SuppressWarnings("null")
    @Test
    void shouldEvictSinglePriceCacheEntry() {
        cacheEvictionService.evictPriceCache(symbol);

        assertThat(cache("current-prices").get(symbol.getPrimaryId())).isNull();
    }

    @SuppressWarnings("null")
    @Test
    void shouldEvictAllPriceCacheEntries() {
        cacheEvictionService.evictAllPriceCache();

        assertThat(cache("current-prices").get(symbol.getPrimaryId())).isNull();
    }

    @SuppressWarnings("null")
    @Test
    void shouldEvictAssetInfoCacheEntry() {
        cacheEvictionService.evictAssetInfoCache(symbol);

        assertThat(cache("asset-info").get(symbol.getPrimaryId())).isNull();
    }

    @SuppressWarnings("null")
    @Test
    void shouldEvictAllCaches() {
        cacheEvictionService.evictAllCaches();

        assertThat(cache("current-prices").get(symbol.getPrimaryId())).isNull();
        assertThat(cache("asset-info").get(symbol.getPrimaryId())).isNull();
        assertThat(cache("historical-prices").get("ANY")).isNull();
        assertThat(cache("trading-currency").get("USD")).isNull();
    }

    private Cache cache(String name) {
        @SuppressWarnings("null")
        Cache cache = cacheManager.getCache(name);
        assertThat(cache)
                .as("Cache " + name + " should exist")
                .isNotNull();
        return cache;
    }

    // 👇 Test-only cache configuration
    @Configuration
    @EnableCaching
    static class TestCacheConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "current-prices",
                    "historical-prices",
                    "asset-info",
                    "trading-currency"
            );
        }
    }
}
