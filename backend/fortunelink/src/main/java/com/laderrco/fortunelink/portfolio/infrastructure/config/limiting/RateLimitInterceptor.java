package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private final ProxyManager<String> proxyManager;
  private final BucketConfiguration globalBucketConfig;
  private final BucketConfiguration marketDataPriceConfig; // Inject the specific bean
  private final BucketConfiguration csvImportConfig; // Inject the specific bean

  public RateLimitInterceptor(ProxyManager<String> proxyManager,
      BucketConfiguration globalBucketConfig,
      @Qualifier("marketDataPriceConfig") BucketConfiguration marketDataPriceConfig,
      @Qualifier("csvImportConfig") BucketConfiguration csvImportConfig) {
    this.proxyManager = proxyManager;
    this.globalBucketConfig = globalBucketConfig;
    this.marketDataPriceConfig = marketDataPriceConfig;
    this.csvImportConfig = csvImportConfig;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    String clientIp = getClientIp(request);
    String uri = request.getRequestURI();

    BucketConfiguration config;
    String limitType;

    if (uri.startsWith("/api/v1/market-data/price")) {
      config = marketDataPriceConfig;
      limitType = "market";
    } else if (uri.startsWith("/api/v1/portfolios") && uri.endsWith("/import")) {
      config = csvImportConfig;
      limitType = "csvImport";
    } else {
      config = globalBucketConfig;
      limitType = "global";
    }

    String cacheKey = clientIp + ":" + limitType;

    Bucket bucket = proxyManager.builder().build(cacheKey, () -> config);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      return true;
    } else {
      response.setStatus(429);
      response.addHeader("X-Rate-Limit-Retry-After-Seconds",
          String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
      return false;
    }
  }

  // Rate limiting uses RemoteAddr directly. X-Forwarded-For is intentionally
  // NOT trusted until a load balancer with a known IP range is in place.
  // See: https://github.com/D3rrickLa/FortuneLink/issues/160
  private String getClientIp(HttpServletRequest request) {
    // TODO: When Nginx/ALB is deployed, re-enable X-Forwarded-For
    // but ONLY after validating request.getRemoteAddr() matches the
    // known load balancer CIDR range.
    return request.getRemoteAddr();
  }
}