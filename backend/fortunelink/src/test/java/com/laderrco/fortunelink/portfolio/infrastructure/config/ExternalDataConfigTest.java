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
    // Instantiate the bean from your config
    ExternalDataConfig config = new ExternalDataConfig();
    restTemplate = config.marketDataRestTemplate();

    // Bind the mock server to this specific restTemplate instance
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void shouldAddCorrelationIdHeaderFromMDC() {
    // 1. Setup MDC
    String expectedId = "test-uuid-1234";
    MDC.put("correlationId", expectedId);

    try {
      // 2. Define expectations: When a GET is made, check for the header
      mockServer.expect(requestTo("/api/market-data"))
          .andExpect(header("X-Request-ID", expectedId))
          .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

      // 3. Execute call
      restTemplate.getForObject("/api/market-data", String.class);

      // 4. Verify all expectations were met
      mockServer.verify();
    } finally {
      // Always clean up MDC to prevent leak to other tests
      MDC.clear();
    }
  }

  @Test
  void shouldNotAddHeaderIfMdcIsEmpty() {
    mockServer.expect(requestTo("/api/market-data"))
        // Ensure the header is NOT present
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