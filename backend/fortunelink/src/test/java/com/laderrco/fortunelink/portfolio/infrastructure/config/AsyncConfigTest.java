package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AsyncConfig.class)
class AsyncConfigTest {

  @Autowired
  private ApplicationContext context;

  @Test
  void shouldConfigureRecalculationExecutorCorrectly() {
    
    ThreadPoolTaskExecutor executor = context.getBean("recalculationExecutor", ThreadPoolTaskExecutor.class);

    
    assertThat(executor.getCorePoolSize()).isEqualTo(2);
    assertThat(executor.getMaxPoolSize()).isEqualTo(4);
    assertThat(executor.getThreadNamePrefix()).isEqualTo("recalc-");

    
    assertThat(executor.getThreadPoolExecutor()).isNotNull();
  }
}