package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private final ProxyManager<String> proxyManager;
  private final BucketConfiguration globalBucketConfig;

  public RateLimitInterceptor(ProxyManager<String> proxyManager, BucketConfiguration globalBucketConfig) {
    this.proxyManager = proxyManager;
    this.globalBucketConfig = globalBucketConfig;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String clientIp = getClientIp(request);

    Bucket bucket = proxyManager.builder().build(clientIp, () -> globalBucketConfig);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      return true;
    } else {
      response.setStatus(429);
      return false;
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}