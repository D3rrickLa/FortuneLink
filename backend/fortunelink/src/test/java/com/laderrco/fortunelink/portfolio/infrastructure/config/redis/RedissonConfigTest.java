package com.laderrco.fortunelink.portfolio.infrastructure.config.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedissonConfigTest {

  @SuppressWarnings("resource")
  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379);

  @Test
  void shouldInitializeClientWithRealRedis() {
    String host = REDIS.getHost();
    Integer port = REDIS.getMappedPort(6379);

    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RedissonConfig.class))
        .withPropertyValues(
            "spring.data.redis.host=" + host,
            "spring.data.redis.port=" + port)
        .run(context -> {
          assertThat(context).hasSingleBean(RedissonClient.class);
          RedissonClient client = context.getBean(RedissonClient.class);
          assertThat(client.getKeys().count()).isNotNull(); // Proves connection works
        });
  }
}