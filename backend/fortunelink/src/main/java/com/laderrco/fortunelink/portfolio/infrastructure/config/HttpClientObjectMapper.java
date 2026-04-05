package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class HttpClientObjectMapper {
  /**
   * Standard ObjectMapper for REST API calls (FMP, etc.) NO activateDefaultTyping here!
   */
  @Bean
  @Primary // This makes it the default for @Autowired ObjectMapper
  public ObjectMapper defaultObjectMapper() {
    return JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
  }
}
