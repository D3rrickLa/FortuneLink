package com.laderrco.fortunelink;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.laderrco.fortunelink.portfolio.application.services.PositionRecalculationService;
import com.laderrco.fortunelink.portfolio.application.services.redislock.RedissonLockProvider;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class FortunelinkApplicationTests {
  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @MockitoBean
  private ProxyManager<String> proxyManager;

  @MockitoBean
  private MarketDataService marketDataService;

  @MockitoBean
  private PositionRecalculationService positionRecalculationService;

  @MockitoBean
  private RedissonLockProvider redissonLockProvider;

  @MockitoBean
  private RedissonClient redissonClient;

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;



  @Test
  void contextLoads() {
    // Now it will pass because Flyway runs against 'postgres'
    // and creates the columns Hibernate expects.
  }
}