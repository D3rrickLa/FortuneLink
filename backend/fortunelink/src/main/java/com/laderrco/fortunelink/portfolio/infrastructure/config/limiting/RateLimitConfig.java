package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import lombok.Data;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

  @Value("${fortunelink.rate-limit.global.requests-per-minute:60}")
  private int globalRequestsPerMinute;

  @Value("${fortunelink.rate-limit.global.requests-per-hour:1000}")
  private int globalRequestsPerHour;

  @Value("${fortunelink.rate-limit.global.requests-per-day:10000}")
  private int globalRequestsPerDay;

  @Bean
  public CommandAsyncExecutor commandAsyncExecutor(RedissonClient redisson) {
    return ((Redisson) redisson).getCommandExecutor();
  }

  @Bean
  public ProxyManager<String> bucketProxyManager(CommandAsyncExecutor executor) {
    return Bucket4jRedisson
        .casBasedBuilder(executor)
        .expirationAfterWrite(
            ExpirationAfterWriteStrategy
                .basedOnTimeForRefillingBucketUpToMax(Duration.ofDays(1)))
        .build();
  }

  @Bean
  public BucketConfiguration globalBucketConfig() {
    return BucketConfiguration.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(globalRequestsPerMinute)
                .refillIntervally(globalRequestsPerMinute, Duration.ofMinutes(1))
                .build())
        .addLimit(
            Bandwidth.builder()
                .capacity(globalRequestsPerHour)
                .refillIntervally(globalRequestsPerHour, Duration.ofHours(1))
                .build())
        .addLimit(
            Bandwidth.builder()
                .capacity(globalRequestsPerDay)
                .refillIntervally(globalRequestsPerDay, Duration.ofDays(1))
                .build())
        .build();
  }

  @Bean
  public BucketConfiguration marketDataPriceConfig() {
    return BucketConfiguration.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(30)
                .refillIntervally(30, Duration.ofMinutes(1))
                .build())
        .build();
  }
}