package com.laderrco.fortunelink.portfolio.infrastructure.config.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter.TtlFunction;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisCacheConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(RedisCacheConfig.class))
      .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
      .withPropertyValues(
          "fortunelink.cache.ttl.current-prices=60",
          "fortunelink.cache.ttl.historical-prices=3600",
          "fortunelink.cache.ttl.asset-info=86400",
          "fortunelink.cache.ttl.trading-currency=43200",
          "fortunelink.cache.ttl.buy-fees=300",
          "fortunelink.cache.key-prefix.prices=prices",
          "fortunelink.cache.key-prefix.historical=hist",
          "fortunelink.cache.key-prefix.asset-info=assets",
          "fortunelink.cache.key-prefix.currency=curr",
          "fortunelink.cache.key-prefix.buy-fees=fees");

  @Test
  void shouldConfigureCacheManagerWithCorrectTTLs() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(CacheManager.class);
      RedisCacheManager cacheManager = context.getBean(RedisCacheManager.class);

      // Get the raw configurations map (bypasses TransactionAware decorator issues)
      Map<String, RedisCacheConfiguration> configs = (Map<String, RedisCacheConfiguration>) ReflectionTestUtils
          .getField(cacheManager, "initialCacheConfiguration");

      assertThat(configs).isNotNull();

      // Verify TTLs using our new helper
      assertThat(getTtl(configs.get("prices"))).isEqualTo(Duration.ofSeconds(60));
      assertThat(getTtl(configs.get("assets"))).isEqualTo(Duration.ofSeconds(86400));
      assertThat(getTtl(configs.get("fees"))).isEqualTo(Duration.ofSeconds(300));
    });
  }

  @Test
  void shouldConfigureCustomObjectMapper() {
    contextRunner.run(context -> {
      // Request by name and cast to the base ObjectMapper class
      Object bean = context.getBean("redisCacheObjectMapper");
      assertThat(bean).isInstanceOf(ObjectMapper.class);

      ObjectMapper mapper = (ObjectMapper) bean;

      // Behavioral test: verify it ignores getters
      var sample = new Object() {
        @SuppressWarnings("unused")
        public String id = "123";

        @SuppressWarnings("unused")
        public String getName() {
          return "Test";
        }
      };

      String json = mapper.writeValueAsString(sample);
      assertThat(json).contains("\"id\":\"123\"");
      assertThat(json).doesNotContain("\"name\"");
    });
  }

  @Test
  void shouldConfigureRedisTemplates() {
    contextRunner.run(context -> {
      assertThat(context).hasBean("marketAssetQuoteRedisTemplate");
      assertThat(context).hasBean("marketAssetIntoRedisTemplate");

      RedisTemplate<?, ?> quoteTemplate = context.getBean("marketAssetQuoteRedisTemplate", RedisTemplate.class);
      assertThat(quoteTemplate.getKeySerializer()).isNotNull();
    });
  }

  private Duration getTtl(RedisCacheConfiguration config) {
    // 1. Use the public getter from the source you provided
    TtlFunction ttlFunction = config.getTtlFunction();

    // 2. Invoke the function.
    // For fixed TTLs (like the ones in your config), the arguments (key, value)
    // are ignored, so passing null is safe.
    return ttlFunction.getTimeToLive(null, null);
  }
}