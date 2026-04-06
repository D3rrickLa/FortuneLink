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

  /**
   * @implNote An attacker can set X-Forwarded-For: 1.2.3.4 in their request and
   *           cycle through "IPs." Only trust this header when behind a known
   *           proxy. Either validate the connecting IP is your load balancer, or
   *           limit it to a single forward
   * @param request
   * @return
   */
  private String getClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      // Take only the FIRST IP, the rest are proxies and can be forged
      String first = forwarded.split(",")[0].trim();
      // Basic sanity check
      if (first.matches("[0-9a-fA-F.:]+"))
        return first;
    }
    return request.getRemoteAddr();
  }
}