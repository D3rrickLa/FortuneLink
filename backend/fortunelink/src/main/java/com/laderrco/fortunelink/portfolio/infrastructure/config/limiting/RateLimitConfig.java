package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Rate limiting configuration using Bucket4j (Token Bucket algorithm).
 * <p>
 * Rate Limit Strategy: - Global: 60 requests/minute per IP - Per-endpoint:
 * Different limits based
 * on operation cost - FMP quota protection: Track daily API usage
 * <p>
 * How Token Bucket Works: 1. Each user gets a "bucket" of tokens 2. Each
 * request consumes 1 token
 * 3. Tokens refill at a fixed rate 4. If bucket is empty, request is rejected
 * (429)
 */
@Data
@Configuration
public class RateLimitConfig {
  @Value("${fortunelink.rate-limit.global.requests-per-minute:60}")
  private int globalRequestsPerMinute;

  @Value("${fortunelink.rate-limit.global.requests-per-hour:1000}")
  private int globalRequestsPerHour;

  @Value("${fortunelink.rate-limit.global.requests-per-day:10000}")
  private int globalRequestsPerDay;

  /**
   * In-memory bucket storage (per IP address). For production, consider
   * Redis-backed buckets for
   * distributed systems.
   */
  @Bean
  public Cache<String, Bucket> rateLimitBuckets() {
    return Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();
  }

  /**
   * Creates a rate limit bucket for an IP address.
   * <p>
   * Uses multiple bandwidth limits (AND condition): - 60 requests per minute -
   * 1000 requests per
   * hour - 10000 requests per day
   * <p>
   * All limits must be satisfied for request to proceed.
   */
  public Bucket createBucket() {
    return Bucket.builder()
        // Limit 1: Per Minute
        .addLimit(limit -> limit.capacity(globalRequestsPerMinute)
            .refillIntervally(globalRequestsPerMinute, Duration.ofMinutes(1)))

        // Limit 2: Per Hour
        .addLimit(limit -> limit.capacity(globalRequestsPerHour)
            .refillIntervally(globalRequestsPerHour, Duration.ofHours(1)))

        // Limit 3: Per Day
        .addLimit(limit -> limit.capacity(globalRequestsPerDay)
            .refillIntervally(globalRequestsPerDay, Duration.ofDays(1)))
        .build();
  }

  /**
   * Endpoint-specific bucket configurations.
   */

  /**
   * Market data price endpoint: 30 requests/minute. Less strict than global
   * (cheap operation,
   * cached).
   */
  public Bucket createMarketDataPriceBucket() {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(30).refillIntervally(30, Duration.ofMinutes(1))).build();
  }

  /**
   * Market data batch endpoint: 10 requests/minute. More expensive operation
   * (multiple symbols).
   */
  public Bucket createMarketDataBatchBucket() {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(10).refillIntervally(10, Duration.ofMinutes(1))).build();
  }

  /**
   * Portfolio write operations: 20 requests/minute. Database writes are more
   * expensive.
   */
  public Bucket createPortfolioWriteBucket() {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(20).refillIntervally(20, Duration.ofMinutes(1))).build();
  }
}