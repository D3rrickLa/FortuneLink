package com.laderrco.fortunelink.portfolio.infrastructure.config.redis;

import java.time.Duration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.EqualJitterDelay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

  @Value("${spring.data.redis.host}")
  private String host;

  @Value("${spring.data.redis.port}")
  private int port;

  @Value("${spring.data.redis.password:}")
  private String password;

  @Lazy
  @Bean(destroyMethod = "shutdown")
  public RedissonClient redissonClient() {
    Config config = new Config();
    String address = "redis://" + host + ":" + port;

    config.useSingleServer().setAddress(address).setConnectionMinimumIdleSize(2)
        .setConnectionPoolSize(10).setTimeout(2000).setRetryAttempts(3)
        .setRetryDelay(new EqualJitterDelay(Duration.ofMillis(500), Duration.ofMillis(1000)));

    if (password != null && !password.trim().isEmpty()) {
      config.setPassword(password);
    }

    return Redisson.create(config);
  }
}