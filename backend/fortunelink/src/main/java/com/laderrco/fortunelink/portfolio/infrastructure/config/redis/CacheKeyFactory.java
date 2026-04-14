package com.laderrco.fortunelink.portfolio.infrastructure.config.redis;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class CacheKeyFactory {
  @Value("${fortunelink.cache.key-prefix.prices}")
  private String pricesPrefix;

  @Value("${fortunelink.cache.key-prefix.asset-info}")
  private String assetInfoPrefix;

  @Value("${fortunelink.cache.key-prefix.historical}")
  private String historicalPrefix;

  @Value("${fortunelink.cache.key-prefix.currency}")
  private String currencyPrefix;

  public String price(String symbol) {
    return pricesPrefix + "::" + symbol;
  }

  public String assetInfo(String symbol) {
    return assetInfoPrefix + "::" + symbol;
  }

  public String historical(String symbol, Instant date) {
    return historicalPrefix + "::" + symbol + "::" + date.toEpochMilli();
  }

  public String currency(String symbol) {
    return currencyPrefix + "::" + symbol;
  }
}