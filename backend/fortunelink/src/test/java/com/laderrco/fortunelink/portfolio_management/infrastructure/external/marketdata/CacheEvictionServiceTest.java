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
import org.springframework.test.context.ActiveProfiles;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.BaseIntegrationTest;


@SpringBootTest
@ActiveProfiles("test")
class CacheEvictionServiceTest extends BaseIntegrationTest {

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

        // This now puts data into a REAL Redis container!
        cache("current-prices").put(symbol.getPrimaryId(), "price");
        cache("asset-info").put(symbol.getPrimaryId(), "info");
        cache("historical-prices").put("ANY", "history");
        cache("trading-currency").put("USD", "currency");
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
        assertThat(cache).as("Cache " + name + " should exist").isNotNull();
        return cache;
    }
    
    // Notice: TestCacheConfig is GONE. We are using the real RedisCacheConfig now.
}