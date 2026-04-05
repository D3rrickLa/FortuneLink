package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class FmpInfrastructureConfig {
  @Bean(name = "fmpHttpClient")
  public HttpClient fmpHttpClient(FmpClientConfig config) {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds())).build();
  }

  @Bean(name = "fmpObjectMapper")
  public ObjectMapper fmpObjectMapper() {
    return JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
  }
}