package com.laderrco.fortunelink.portfolio.infrastructure.config.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.CurrencyDeserializer;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.CurrencySerializer;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.MarketAssetInfoDeserializer;
import com.laderrco.fortunelink.portfolio.infrastructure.config.serialization.MarketAssetInfoSerializer;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Configuration
@EnableCaching
public class RedisCacheConfig {
  // --- TTL Values ---
  @Value("${fortunelink.cache.ttl.current-prices}")
  private long currentPricesTtl;

  @Value("${fortunelink.cache.ttl.historical-prices}")
  private long historicalPricesTtl;

  @Value("${fortunelink.cache.ttl.asset-info}")
  private long assetInfoTtl;

  @Value("${fortunelink.cache.ttl.trading-currency}")
  private long tradingCurrencyTtl;

  // --- Cache Names (Prefixes) ---
  @Value("${fortunelink.cache.key-prefix.prices}")
  private String pricesCacheName;

  @Value("${fortunelink.cache.key-prefix.historical}")
  private String historicalCacheName;

  @Value("${fortunelink.cache.key-prefix.asset-info}")
  private String assetInfoCacheName;

  @Value("${fortunelink.cache.key-prefix.currency}")
  private String currencyCacheName;

  @Value("${fortunelink.cache.ttl.buy-fees}")
  private long buyFeesTtl;

  @Value("${fortunelink.cache.key-prefix.buy-fees}")
  private String buyFeesCacheName;

  @Bean
  public RedisTemplate<String, MarketAssetQuote> marketAssetQuoteRedisTemplate(
      RedisConnectionFactory connectionFactory,
      @Qualifier("redisCacheObjectMapper") ObjectMapper objectMapper) {

    RedisTemplate<String, MarketAssetQuote> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    template.setKeySerializer(new StringRedisSerializer());

    JacksonJsonRedisSerializer<MarketAssetQuote> serializer = new JacksonJsonRedisSerializer<>(
        objectMapper, MarketAssetQuote.class);

    template.setValueSerializer(serializer);
    template.setHashValueSerializer(serializer);

    return template;
  }

  @Bean(name = "redisCacheObjectMapper")
  public JsonMapper redisCacheObjectMapper() {
    SimpleModule module = new SimpleModule().addSerializer(MarketAssetInfo.class,
        new MarketAssetInfoSerializer())
        .addDeserializer(MarketAssetInfo.class, new MarketAssetInfoDeserializer())
        .addSerializer(Currency.class, new CurrencySerializer())
        .addDeserializer(Currency.class, new CurrencyDeserializer());

    return JsonMapper.builder().addModule(module)
        .changeDefaultVisibility(vc -> vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
        .changeDefaultVisibility(vc -> vc.withGetterVisibility(JsonAutoDetect.Visibility.NONE))
        .changeDefaultVisibility(vc -> vc.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
      @Qualifier("redisCacheObjectMapper") JsonMapper objectMapper) {

    StringRedisSerializer keySerializer = new StringRedisSerializer();
    GenericJacksonJsonRedisSerializer genericValueSerializer = new GenericJacksonJsonRedisSerializer(
        objectMapper);

    JacksonJsonRedisSerializer<MarketAssetQuote> marketAssetQuoteSerializer = new JacksonJsonRedisSerializer<>(
        objectMapper, MarketAssetQuote.class);

    JacksonJsonRedisSerializer<MarketAssetInfo> marketAssetInfoSerializer = new JacksonJsonRedisSerializer<>(
        objectMapper, MarketAssetInfo.class);

    RedisCacheConfiguration defaultConfig = getDefaultConfig(keySerializer, genericValueSerializer);

    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

    // Map the property-driven names to their specific TTLs and Serializers
    cacheConfigs.put(pricesCacheName, defaultConfig.entryTtl(Duration.ofSeconds(currentPricesTtl))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(marketAssetQuoteSerializer)));

    cacheConfigs.put(historicalCacheName,
        defaultConfig.entryTtl(Duration.ofSeconds(historicalPricesTtl)));

    cacheConfigs.put(assetInfoCacheName, defaultConfig.entryTtl(Duration.ofSeconds(assetInfoTtl))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(marketAssetInfoSerializer)));

    cacheConfigs.put(currencyCacheName,
        defaultConfig.entryTtl(Duration.ofSeconds(tradingCurrencyTtl)));

    cacheConfigs.put(buyFeesCacheName, defaultConfig.entryTtl(Duration.ofSeconds(buyFeesTtl)));

    return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs).transactionAware().build();
  }

  private RedisCacheConfiguration getDefaultConfig(StringRedisSerializer keySerializer,
      GenericJacksonJsonRedisSerializer genericValueSerializer) {
    return RedisCacheConfiguration.defaultCacheConfig().serializeKeysWith(
        RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(genericValueSerializer))
        .disableCachingNullValues();
  }
}