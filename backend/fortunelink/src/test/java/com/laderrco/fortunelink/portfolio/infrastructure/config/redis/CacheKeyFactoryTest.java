package com.laderrco.fortunelink.portfolio.infrastructure.config.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CacheKeyFactoryTest {

  private CacheKeyFactory factory;

  @BeforeEach
  void setUp() {
    factory = new CacheKeyFactory();
    
    ReflectionTestUtils.setField(factory, "pricesPrefix", "prices");
    ReflectionTestUtils.setField(factory, "assetInfoPrefix", "assets");
    ReflectionTestUtils.setField(factory, "historicalPrefix", "hist");
    ReflectionTestUtils.setField(factory, "currencyPrefix", "curr");
  }

  @Test
  void shouldGeneratePriceKey() {
    assertThat(factory.price("BTC")).isEqualTo("prices::BTC");
  }

  @Test
  void shouldGenerateAssetInfoKey() {
    assertThat(factory.assetInfo("ETH")).isEqualTo("assets::ETH");
  }

  @Test
  void shouldGenerateHistoricalKey() {
    Instant now = Instant.now();
    String expected = "hist::BTC::" + now.toEpochMilli();
    assertThat(factory.historical("BTC", now)).isEqualTo(expected);
  }

  @Test
  void shouldGenerateCurrencyKey() {
    assertThat(factory.currency("USD")).isEqualTo("curr::USD");
  }
}