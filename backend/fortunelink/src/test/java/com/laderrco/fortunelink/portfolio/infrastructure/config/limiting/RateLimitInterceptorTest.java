package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

  private MockMvc mockMvc;

  @Mock
  private ProxyManager<String> proxyManager;
  @Mock
  private RemoteBucketBuilder<String> bucketBuilder;
  @Mock
  private BucketProxy mockBucket;
  @Mock
  private ConsumptionProbe probe;
  @Mock
  private BucketConfiguration globalConfig;
  @Mock
  private BucketConfiguration marketConfig;
  @Mock
  private BucketConfiguration csvConfig;

  @BeforeEach
  void setUp() {
    RateLimitInterceptor interceptor = new RateLimitInterceptor(
        proxyManager, globalConfig, marketConfig, csvConfig);

    this.mockMvc = MockMvcBuilders
        .standaloneSetup(new TestController())
        .addInterceptors(interceptor)
        .build();

    when(proxyManager.builder()).thenReturn(bucketBuilder);
    when(bucketBuilder.build(anyString(), any(Supplier.class)))
        .thenReturn(mockBucket);
  }

  @Test
  void shouldAllowRequestWhenTokensAvailable() throws Exception {
    when(mockBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);
    when(probe.getRemainingTokens()).thenReturn(5L);

    mockMvc.perform(get("/api/v1/any-url")
        .remoteAddress("127.0.0.1"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Rate-Limit-Remaining", "5"));
  }

  @Test
  void shouldFallbackToGlobalWhenPortfolioPathDoesNotEndWithImport() throws Exception {
    when(mockBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);

    
    mockMvc.perform(get("/api/v1/portfolios/123/details")
        .remoteAddress("2.2.2.2"))
        .andExpect(status().isOk());

    
    verify(bucketBuilder).build(eq("2.2.2.2:global"), any(java.util.function.Supplier.class));
  }

  @Test
  void shouldVerifyGlobalFallbackPath() throws Exception {
    when(mockBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);

    mockMvc.perform(get("/api/v1/other")
        .remoteAddress("3.3.3.3"))
        .andExpect(status().isOk());

    verify(bucketBuilder).build(
        eq("3.3.3.3:global"),
        argThat((Supplier<BucketConfiguration> s) -> s.get() == globalConfig));
  }

  @Test
  void shouldRejectWith429WhenLimitExceeded() throws Exception {
    when(mockBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(false);
    when(probe.getNanosToWaitForRefill()).thenReturn(5_000_000_000L); 

    mockMvc.perform(get("/api/v1/any-url")
        .remoteAddress("127.0.0.1"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("X-Rate-Limit-Retry-After-Seconds", "5"));
  }

  @Test
  void shouldVerifySpecificMarketDataPath() throws Exception {
    when(mockBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);

    mockMvc.perform(get("/api/v1/market-data/price")
        .remoteAddress("192.168.1.1"))
        .andExpect(status().isOk());

    verify(bucketBuilder).build(eq("192.168.1.1:market"), any(java.util.function.Supplier.class));
  }

  @Test
  void shouldVerifyCsvImportPath() throws Exception {
    when(mockBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);

    mockMvc.perform(get("/api/v1/portfolios/123/import")
        .remoteAddress("1.1.1.1"))
        .andExpect(status().isOk());

    verify(bucketBuilder).build(eq("1.1.1.1:csvImport"), any(java.util.function.Supplier.class));
  }

  
  @RestController
  private static class TestController {
    @GetMapping("/**")
    public void handle() {
    }
  }
}