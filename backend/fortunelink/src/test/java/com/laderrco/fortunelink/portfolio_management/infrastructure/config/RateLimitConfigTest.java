package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.bucket4j.Bucket;

@SpringBootTest(properties = {
        "fortunelink.rate-limit.global.requests-per-minute=60",
        "fortunelink.rate-limit.global.requests-per-hour=1000",
        "fortunelink.rate-limit.global.requests-per-day=10000"
    }, classes = RateLimitConfig.class)
class RateLimitConfigTest {

    @Autowired
    private RateLimitConfig config;

    @Test
    void rateLimitBuckets_ShouldBeEmptyMap() {
        assertNotNull(config.rateLimitBuckets());
        assertTrue(config.rateLimitBuckets().isEmpty());
    }

    @Test
    void createBucket_ShouldReturnNonNullBucket() {
        Bucket bucket = config.createBucket();
        assertNotNull(bucket);
    }

    @Test
    void createMarketDataPriceBucket_ShouldReturnNonNullBucket() {
        Bucket bucket = config.createMarketDataPriceBucket();
        assertNotNull(bucket);
    }

    @Test
    void marketDataBatchBucket_ShouldAllow10RequestsPerMinute() {
        Bucket bucket = config.createMarketDataBatchBucket();
        assertNotNull(bucket);

        // Consume 10 tokens successfully
        for (int i = 0; i < 10; i++) {
            assertTrue(bucket.tryConsume(1), "Token " + (i + 1) + " should be allowed");
        }

        // 11th token should be blocked
        assertFalse(bucket.tryConsume(1), "11th token should be blocked");
    }

    @Test
    void portfolioWriteBucket_ShouldAllow20RequestsPerMinute() {
        Bucket bucket = config.createPortfolioWriteBucket();
        assertNotNull(bucket);

        // Consume 20 tokens successfully
        for (int i = 0; i < 20; i++) {
            assertTrue(bucket.tryConsume(1), "Token " + (i + 1) + " should be allowed");
        }

        // 21st token should be blocked
        assertFalse(bucket.tryConsume(1), "21st token should be blocked");
    }
}