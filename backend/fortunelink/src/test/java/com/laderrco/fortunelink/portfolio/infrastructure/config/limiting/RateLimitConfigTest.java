package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import io.github.bucket4j.BucketConfiguration;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(RateLimitConfig.class))
      // This allows the test beans to override the ones in RateLimitConfig
      .withAllowBeanDefinitionOverriding(true)
      .withBean(org.redisson.command.CommandAsyncExecutor.class,
          () -> Mockito.mock(org.redisson.command.CommandAsyncExecutor.class))
      .withBean(RedissonClient.class,
          () -> Mockito.mock(RedissonClient.class));

  @Test
  void shouldNotLoadConfigWhenDisabled() {
    contextRunner
        .withPropertyValues("fortunelink.rate-limit.enabled=false")
        .run(context -> {
          assertThat(context).doesNotHaveBean(RateLimitConfig.class);
        });
  }

  @Test
  void shouldLoadConfigByDefault() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(RateLimitConfig.class);
      assertThat(context).hasBean("globalBucketConfig");
    });
  }

  @Test
  void verifyGlobalBucketLimits() {
    contextRunner
        .withPropertyValues(
            "fortunelink.rate-limit.global.requests-per-minute=10",
            "fortunelink.rate-limit.global.requests-per-hour=100")
        .run(context -> {
          BucketConfiguration config = context.getBean("globalBucketConfig", BucketConfiguration.class);

          assertThat(config.getBandwidths()).hasSize(3);
          assertThat(config.getBandwidths()[0].getCapacity()).isEqualTo(10);
          assertThat(config.getBandwidths()[1].getCapacity()).isEqualTo(100);
          assertThat(config.getBandwidths()[2].getCapacity()).isEqualTo(10000);
        });
  }

  @Test
  void verifySpecificServiceConfigs() {
    contextRunner.run(context -> {
      BucketConfiguration marketConfig = context.getBean("marketDataPriceConfig", BucketConfiguration.class);
      assertThat(marketConfig.getBandwidths()[0].getCapacity()).isEqualTo(30);

      BucketConfiguration csvConfig = context.getBean("csvImportConfig", BucketConfiguration.class);
      assertThat(csvConfig.getBandwidths()[0].getCapacity()).isEqualTo(3);
    });
  }
}