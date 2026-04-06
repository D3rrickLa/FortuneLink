package com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;

@Component
public class CaffeineIdempotencyCache implements IdempotencyCache {
  // Store keys for 24 hours. After that, we assume the client
  // won't retry that specific request anymore.
  private final Cache<String, TransactionView> cache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofHours(24))
      .maximumSize(10_000)
      .build();

  @Override
  public TransactionView get(String key) {
    return cache.getIfPresent(key);
  }

  @Override
  public void put(String key, TransactionView value) {
    cache.put(key, value);
  }
}
