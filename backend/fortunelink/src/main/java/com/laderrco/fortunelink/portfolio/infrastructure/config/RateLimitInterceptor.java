package com.laderrco.fortunelink.portfolio.infrastructure.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * HTTP interceptor that enforces rate limits on API endpoints.
 * <p>
 * How it works: 1. Extract client IP from request 2. Get or create rate limit bucket for that IP 3.
 * Try to consume 1 token from bucket 4. If successful → allow request 5. If failed → reject with
 * 429 Too Many Requests
 * <p>
 * Headers added to response: - X-Rate-Limit-Remaining: Tokens left -
 * X-Rate-Limit-Retry-After-Seconds: Wait time until next token
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

  private final RateLimitConfig rateLimitConfig;
  private final Map<String, Bucket> buckets;

  @Value("${fortunelink.rate-limit.enabled:true}")
  private boolean rateLimitEnabled;

  public RateLimitInterceptor(RateLimitConfig rateLimitConfig, Map<String, Bucket> buckets) {
    this.rateLimitConfig = rateLimitConfig;
    this.buckets = buckets;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    // Skip rate limiting if disabled (dev environment)
    if (!rateLimitEnabled) {
      return true;
    }

    // Wrap the requet so Spring cand ready the body after interceptor
    if (!(request instanceof ContentCachingRequestWrapper)) {
      Objects.requireNonNull(request);
      request = new ContentCachingRequestWrapper(request, 1);
    }

    // Get client IP
    String clientIp = getClientIp(request);

    // Get or create bucket for this IP
    Bucket bucket = buckets.computeIfAbsent(clientIp, k -> rateLimitConfig.createBucket());

    // Try to consume 1 token
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      // Request allowed - add rate limit headers
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      log.debug("Rate limit check passed for IP: {} (remaining: {})", clientIp,
          probe.getRemainingTokens());
      return true;
    } else {
      // Request denied - bucket is empty
      long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

      response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
      response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
          "Rate limit exceeded. Please try again in " + waitForRefill + " seconds.");

      log.warn("Rate limit exceeded for IP: {} (retry after {} seconds)", clientIp, waitForRefill);
      return false;
    }
  }

  /**
   * Extract client IP address from request. Handles proxies and load balancers (X-Forwarded-For
   * header).
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");

    // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
    // Use the first one (actual client)
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    return request.getRemoteAddr();
  }
}