package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.NonNull;

/**
 * Rdis cach config for App
 * 
 * Configures different cache regions with specific TTLs
 * - current-prices: 5 min
 * - historical-prices: 24 hours
 * - asset-info 7 days
 * - trading-currency 7 days
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {
    @Value("${fortunelink.cache.ttl.current-prices:300}")
    private long currentPricesTtl;

    @Value("${fortunelink.cache.ttl.historical-prices:86400}")
    private long historicalPricesTtl;

    @Value("${fortunelink.cache.ttl.asset-info:604800}")
    private long assetInfoTtl;

    @Value("${fortunelink.cache.ttl.trading-currency:604800}")
    private long tradingCurrencyTtl;

    /**
     * Custom ObjectMapper for Redis serialization.
     * Handles LocalDateTime, BigDecimal, and other domain types.
     */
    @Bean
    public ObjectMapper redisCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    /**
     * Main cache manager with multiple cache configurations.
     */
    @Bean
    @SuppressWarnings("null")
    public CacheManager cacheManager(@NonNull RedisConnectionFactory connectionFactory,
            @NonNull ObjectMapper redisCacheObjectMapper) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(300)) // 5 minutes default
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper)))
                .disableCachingNullValues();

        // Specific cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Current prices - 5 minutes TTL
        cacheConfigurations.put("current-prices",
                defaultConfig.entryTtl(Duration.ofSeconds(currentPricesTtl)));

        // Historical prices - 24 hours TTL (historical data doesn't change)
        cacheConfigurations.put("historical-prices",
                defaultConfig.entryTtl(Duration.ofSeconds(historicalPricesTtl)));

        // Asset info - 7 days TTL (metadata rarely changes)
        cacheConfigurations.put("asset-info",
                defaultConfig.entryTtl(Duration.ofSeconds(assetInfoTtl)));

        // Trading currency - 7 days TTL (currency for symbol rarely changes)
        cacheConfigurations.put("trading-currency",
                defaultConfig.entryTtl(Duration.ofSeconds(tradingCurrencyTtl)));

        // Batch prices - same as current-prices
        cacheConfigurations.put("batch-prices",
                defaultConfig.entryTtl(Duration.ofSeconds(currentPricesTtl)));

        // Batch asset info - same as asset-info
        cacheConfigurations.put("batch-asset-info",
                defaultConfig.entryTtl(Duration.ofSeconds(assetInfoTtl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // Support Spring transactions
                .build();
    }
}
