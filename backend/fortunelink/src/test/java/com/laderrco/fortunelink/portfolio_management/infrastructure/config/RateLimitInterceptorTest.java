package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class RateLimitInterceptorTest {
    public class TestUtils {
        public static void setField(Object target, String fieldName, Object value) {
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private RateLimitConfig config;
    private Map<String, Bucket> buckets;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        config = mock(RateLimitConfig.class); // We'll mock bucket creation
        buckets = new HashMap<>();
        interceptor = new RateLimitInterceptor(config, buckets);

        // Enable rate limiting for tests
        TestUtils.setField(interceptor, "rateLimitEnabled", true);
    }

    @Test
    void preHandle_ShouldAllowRequest_WhenBucketHasTokens() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRemoteAddr()).thenReturn("1.2.3.4");

        // Create a bucket that always consumes successfully
        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(5L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        // Mock RateLimitConfig to return our bucket
        when(config.createBucket()).thenReturn(bucket);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(bucket, buckets.get("1.2.3.4"));

        // Verify header was added
        verify(response).addHeader("X-Rate-Limit-Remaining", "5");
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void preHandle_ShouldBlockRequest_WhenBucketEmpty() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRemoteAddr()).thenReturn("5.6.7.8");

        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(2_000_000_000L); // 2 seconds
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(config.createBucket()).thenReturn(bucket);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).addHeader("X-Rate-Limit-Retry-After-Seconds", "2");
        verify(response).sendError(eq(429), contains("Rate limit exceeded"));
        assertEquals(bucket, buckets.get("5.6.7.8"));
    }

    @Test
    void preHandle_ShouldSkipRateLimit_WhenDisabled() throws Exception {
        TestUtils.setField(interceptor, "rateLimitEnabled", false);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(response);
    }

    @Test
    void preHandle_ShouldUseXForwardedForHeader_IfPresent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getHeader("X-Forwarded-For")).thenReturn("9.8.7.6, 1.2.3.4");

        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(1L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(config.createBucket()).thenReturn(bucket);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(bucket, buckets.get("9.8.7.6"));
        verify(response).addHeader("X-Rate-Limit-Remaining", "1");
    }
}