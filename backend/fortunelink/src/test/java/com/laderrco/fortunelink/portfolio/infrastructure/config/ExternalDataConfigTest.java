package com.laderrco.fortunelink.portfolio.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalDataConfigTest {
  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    
    ExternalDataConfig config = new ExternalDataConfig();
    restTemplate = config.marketDataRestTemplate();

    
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void shouldAddCorrelationIdHeaderFromMDC() {
    
    String expectedId = "test-uuid-1234";
    MDC.put("correlationId", expectedId);

    try {
      
      mockServer.expect(requestTo("/api/market-data"))
          .andExpect(header("X-Request-ID", expectedId))
          .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

      
      restTemplate.getForObject("/api/market-data", String.class);

      
      mockServer.verify();
    } finally {
      
      MDC.clear();
    }
  }

  @Test
  void shouldNotAddHeaderIfMdcIsEmpty() {
    mockServer.expect(requestTo("/api/market-data"))
        
        .andExpect(request -> {
          if (request.getHeaders().containsHeader("X-Request-ID")) {
            throw new AssertionError("X-Request-ID header should not be present");
          }
        })
        .andRespond(withSuccess());

    restTemplate.getForObject("/api/market-data", String.class);
    mockServer.verify();
  }
}