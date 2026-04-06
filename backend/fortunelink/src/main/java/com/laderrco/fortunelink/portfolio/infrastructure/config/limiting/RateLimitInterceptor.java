package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.Cache;


@Component
public class RateLimitInterceptor implements HandlerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

  private final RateLimitConfig rateLimitConfig;
  private final Cache<String, Bucket> buckets; // Use Caffeine Cache, not Map

  @Value("${fortunelink.rate-limit.enabled:true}")
  private boolean rateLimitEnabled;

  // Corrected Constructor
  public RateLimitInterceptor(RateLimitConfig rateLimitConfig, Cache<String, Bucket> buckets) {
    this.rateLimitConfig = rateLimitConfig;
    this.buckets = buckets;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    if (!rateLimitEnabled) {
      return true;
    }

    // Standard IP extraction
    String clientIp = getClientIp(request);

    // USE CAFFEINE'S .get() METHOD
    // This is atomic: if the key doesn't exist, it runs the mapping function
    Bucket bucket = buckets.get(clientIp, k -> rateLimitConfig.createBucket());

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      log.debug("Rate limit passed for IP: {} (remaining: {})", clientIp, probe.getRemainingTokens());
      return true;
    } else {
      long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

      response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.getWriter().write("Rate limit exceeded. Try again in " + waitForRefill + " seconds.");

      log.warn("Rate limit exceeded for IP: {} (retry after {}s)", clientIp, waitForRefill);
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