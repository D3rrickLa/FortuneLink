package com.laderrco.fortunelink.portfolio.infrastructure.config.securitytestsuite;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.api.web.controller.PortfolioController;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioLifecycleService;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortfolioController.class)
@Import(TestSecurityConfig.class)
class PortfolioControllerSliceTest {

  @Autowired
  private MockMvc mockMvc;


  @MockitoBean
  private ProxyManager<String> proxyManager;

  @MockitoBean
  @Qualifier("globalBucketConfig")
  private BucketConfiguration globalBucketConfig;

  @MockitoBean
  @Qualifier("marketDataPriceConfig")
  private BucketConfiguration marketDataPriceConfig;

  @MockitoBean
  @Qualifier("csvImportConfig")
  private BucketConfiguration csvImportConfig;

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;


  @MockitoBean
  private PortfolioLifecycleService lifecycleService;

  @MockitoBean
  private PortfolioQueryService queryService;

  @MockitoBean
  private AuthenticationUserService authenticationUserService;

  @BeforeEach
  void setup() {
    when(authenticationUserService.getCurrentUser()).thenReturn(UUID.randomUUID());
  }

  @Test
  void getPortfolios_authenticated_returns200() throws Exception {
    when(queryService.getPortfolioSummaries(any())).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/portfolios").with(jwt())).andExpect(status().isOk());
  }
}