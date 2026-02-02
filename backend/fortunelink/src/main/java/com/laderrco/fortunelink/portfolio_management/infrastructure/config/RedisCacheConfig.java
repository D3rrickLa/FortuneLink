package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers.MarketAssetInfoDeserializer;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers.MarketAssetInfoSerializer;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers.ValidatedCurrencyDeserializer;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.json_serializers.ValidatedCurrencySerializer;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

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
    @Bean("redisCacheObjectMapper")
    public ObjectMapper redisCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register custom serializers
        SimpleModule module = new SimpleModule();
        module.addSerializer(MarketAssetInfo.class, new MarketAssetInfoSerializer());
        module.addDeserializer(MarketAssetInfo.class, new MarketAssetInfoDeserializer());

        // module.addSerializer(ValidatedCurrency.class, new ValidatedCurrencySerializer());
        // module.addDeserializer(ValidatedCurrency.class, new ValidatedCurrencyDeserializer());

        mapper.registerModule(module);

        // Other modules
        mapper.registerModule(new ParameterNamesModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());

        // Field-based serialization
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);

        return mapper;
    }

    /**
     * Standard ObjectMapper for REST API calls (FMP, etc.)
     */
    @Bean("defaultObjectMapper")
    @Primary // This makes it the default for @Autowired ObjectMapper
    public ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new ParameterNamesModule());  // ADD THIS - needed for Records
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // NO activateDefaultTyping here!
        return mapper;

    }

    /**
     * Main cache manager with multiple cache configurations.
     */
    @Bean
    @SuppressWarnings("null")
    @Qualifier("redisCacheObjectMapper") 
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper redisCacheObjectMapper) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer genericValueSerializer = new GenericJackson2JsonRedisSerializer(
                redisCacheObjectMapper);

        // Typed serializer for Money
        Jackson2JsonRedisSerializer<Money> moneySerializer = new Jackson2JsonRedisSerializer<>(
                redisCacheObjectMapper, Money.class);

        // Typed serializer for MarketAssetInfo (with custom deserializer in
        // ObjectMapper)
        Jackson2JsonRedisSerializer<MarketAssetInfo> marketAssetInfoSerializer = new Jackson2JsonRedisSerializer<>(
                redisCacheObjectMapper, MarketAssetInfo.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericValueSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("current-prices",
                defaultConfig.entryTtl(Duration.ofSeconds(currentPricesTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(moneySerializer)));

        cacheConfigs.put("historical-prices",
                defaultConfig.entryTtl(Duration.ofSeconds(historicalPricesTtl)));

        // Use typed serializer for MarketAssetInfo
        cacheConfigs.put("asset-info",
                defaultConfig.entryTtl(Duration.ofSeconds(assetInfoTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(marketAssetInfoSerializer)));

        cacheConfigs.put("trading-currency",
                defaultConfig.entryTtl(Duration.ofSeconds(tradingCurrencyTtl)));

        cacheConfigs.put("batch-prices",
                defaultConfig.entryTtl(Duration.ofSeconds(currentPricesTtl)));

        cacheConfigs.put("batch-asset-info",
                defaultConfig.entryTtl(Duration.ofSeconds(assetInfoTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(marketAssetInfoSerializer)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
