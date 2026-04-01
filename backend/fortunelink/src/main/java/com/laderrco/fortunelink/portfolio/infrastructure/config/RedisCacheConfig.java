package com.laderrco.fortunelink.portfolio.infrastructure.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.CurrencyDeserializer;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.CurrencySerializer;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.MarketAssetInfoDeserializer;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.MarketAssetInfoSerializer;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;

@Configuration
@EnableCaching
public class RedisCacheConfig {
  @Value("${fortunelink.cache.ttl.current-prices}")
  private long currentPricesTtl;

  @Value("${fortunelink.cache.ttl.historical-prices}")
  private long historicalPricesTtl;

  @Value("${fortunelink.cache.ttl.asset-info}")
  private long assetInfoTtl;

  @Value("${fortunelink.cache.ttl.trading-currency}")
  private long tradingCurrencyTtl;

  /**
   * Custom ObjectMapper for Redis serialization.
   * Handles LocalDateTime, BigDecimal, and other domain types.
   */
  @Bean("redisCacheObjectMapper")
  public ObjectMapper redisCacheObjectMapper() {
    SimpleModule module = new SimpleModule()
        .addSerializer(MarketAssetInfo.class, new MarketAssetInfoSerializer())
        .addDeserializer(MarketAssetInfo.class, new MarketAssetInfoDeserializer())
        .addSerializer(Currency.class, new CurrencySerializer())
        .addDeserializer(Currency.class, new CurrencyDeserializer());

    return JsonMapper.builder()
        .addModule(module)

        // Visibility (Jackson 3 style)
        .changeDefaultVisibility(vc -> vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
        .changeDefaultVisibility(vc -> vc.withGetterVisibility(JsonAutoDetect.Visibility.NONE))
        .changeDefaultVisibility(vc -> vc.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))

        // Safer deserialization
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder().build(),
            DefaultTyping.NON_FINAL)
        .build();
  }

  /**
   * Main cache manager with multiple cache configurations.
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
      @Qualifier("redisCacheObjectMapper") ObjectMapper objectMapper) {

    StringRedisSerializer keySerializer = new StringRedisSerializer();
    GenericJacksonJsonRedisSerializer genericValueSerializer = new GenericJacksonJsonRedisSerializer(
        objectMapper);

    JacksonJsonRedisSerializer<MarketAssetQuote> moneySerializer = new JacksonJsonRedisSerializer<>(
        objectMapper, MarketAssetQuote.class);

    // Typed serializer for MarketAssetInfo (with custom deserializer in ObjectMapper)
    JacksonJsonRedisSerializer<MarketAssetInfo> marketAssetInfoSerializer = new JacksonJsonRedisSerializer<>(
        objectMapper, MarketAssetInfo.class);

    RedisCacheConfiguration defaultConfig = getDefaultConfig(keySerializer, genericValueSerializer);

    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

    cacheConfigs.put("current-prices", defaultConfig.entryTtl(Duration.ofSeconds(currentPricesTtl))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(moneySerializer)));

    cacheConfigs.put("historical-prices", defaultConfig.entryTtl(Duration.ofSeconds(historicalPricesTtl)));

    cacheConfigs.put("asset-info", defaultConfig.entryTtl(Duration.ofSeconds(assetInfoTtl))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(marketAssetInfoSerializer)));

    cacheConfigs.put("trading-currency", defaultConfig.entryTtl(Duration.ofSeconds(tradingCurrencyTtl)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs)
        .transactionAware()
        .build();
  }

  private RedisCacheConfiguration getDefaultConfig(StringRedisSerializer keySerializer,
      GenericJacksonJsonRedisSerializer genericValueSerializer) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericValueSerializer))
        .disableCachingNullValues();
    return defaultConfig;
  }
}
