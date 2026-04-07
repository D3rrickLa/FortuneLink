package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ExternalDataConfig {
  @Bean
  public RestTemplate marketDataRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add((request, body, execution) -> {
      String correlationId = MDC.get("correlationId");
      if (correlationId != null) {
        request.getHeaders().add("X-Request-ID", correlationId);
      }
      return execution.execute(request, body);
    });
    return restTemplate;
  }

}
