package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import java.time.Duration;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Data
@Configuration
@ConditionalOnProperty(name = "fortunelink.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

  @Value("${fortunelink.rate-limit.global.requests-per-minute:60}")
  private int globalRequestsPerMinute;

  @Value("${fortunelink.rate-limit.global.requests-per-hour:1000}")
  private int globalRequestsPerHour;

  @Value("${fortunelink.rate-limit.global.requests-per-day:10000}")
  private int globalRequestsPerDay;

  @Bean
  @ConditionalOnMissingBean(CommandAsyncExecutor.class)
  public CommandAsyncExecutor commandAsyncExecutor(RedissonClient redissonClient) {
    if (redissonClient instanceof Redisson) {
      return ((Redisson) redissonClient).getCommandExecutor();
    }
    throw new IllegalStateException("RedissonClient is not an instance of Redisson implementation");
  }

  @Bean
  public ProxyManager<String> bucketProxyManager(CommandAsyncExecutor commandAsyncExecutor) {
    return Bucket4jRedisson.casBasedBuilder(commandAsyncExecutor).expirationAfterWrite(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofDays(1)))
        .build();
  }

  @Bean
  @Primary
  public BucketConfiguration globalBucketConfig() {
    return BucketConfiguration.builder().addLimit(
        Bandwidth.builder().capacity(globalRequestsPerMinute)
            .refillIntervally(globalRequestsPerMinute, Duration.ofMinutes(1)).build()).addLimit(
        Bandwidth.builder().capacity(globalRequestsPerHour)
            .refillIntervally(globalRequestsPerHour, Duration.ofHours(1)).build()).addLimit(
        Bandwidth.builder().capacity(globalRequestsPerDay)
            .refillIntervally(globalRequestsPerDay, Duration.ofDays(1)).build()).build();
  }

  @Bean("marketDataPriceConfig")
  public BucketConfiguration marketDataPriceConfig() {
    return BucketConfiguration.builder().addLimit(
            Bandwidth.builder().capacity(30).refillIntervally(30, Duration.ofMinutes(1)).build())
        .build();
  }

  @Bean("csvImportConfig")
  public BucketConfiguration csvImportConfig() {
    return BucketConfiguration.builder().addLimit(
        Bandwidth.builder().capacity(3).refillIntervally(3, Duration.ofMinutes(1)).build()).build();
  }
}