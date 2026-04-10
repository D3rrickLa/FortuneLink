package com.laderrco.fortunelink.portfolio.infrastructure.config.redis;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry(proxyTargetClass = true)
@Configuration
public class RetryConfig {


}