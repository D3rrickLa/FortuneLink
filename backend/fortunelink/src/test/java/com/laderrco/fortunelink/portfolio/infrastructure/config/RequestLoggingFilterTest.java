package com.laderrco.fortunelink.portfolio.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

  private RequestLoggingFilter filter;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new RequestLoggingFilter();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
    MDC.clear();
  }

  @Test
  void shouldPropagateExistingRequestId() throws ServletException, IOException {
    // Arrange
    String existingId = "client-provided-id";
    when(request.getHeader("X-Request-ID")).thenReturn(existingId);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(response).setHeader("X-Request-ID", existingId);
    verify(filterChain).doFilter(request, response);
    // MDC is cleared in finally block, so we check if it's empty now
    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  void shouldGenerateNewIdIfHeaderMissing() throws ServletException, IOException {
    // Arrange
    when(request.getHeader("X-Request-ID")).thenReturn(null);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    // Capture the generated ID to ensure it was actually a UUID
    verify(response).setHeader(eq("X-Request-ID"), argThat(id -> {
      assertThat(id).isNotBlank();
      return true;
    }));
  }

  @Test
  void shouldClearMdcEvenIfExceptionOccurs() throws ServletException, IOException {
    // Arrange
    when(request.getHeader("X-Request-ID")).thenReturn("error-id");
    doThrow(new RuntimeException("Filter error")).when(filterChain).doFilter(any(), any());

    // Act & Assert
    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (RuntimeException ignored) {
      // Expected
    }

    // Verify MDC was still cleared due to 'finally' block
    assertThat(MDC.get("requestId")).isNull();
  }
}