package com.laderrco.fortunelink.portfolio.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

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

    String existingId = "client-provided-id";
    when(request.getHeader("X-Request-ID")).thenReturn(existingId);

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setHeader("X-Request-ID", existingId);
    verify(filterChain).doFilter(request, response);

    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  void shouldGenerateNewIdIfHeaderMissing() throws ServletException, IOException {

    when(request.getHeader("X-Request-ID")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setHeader(eq("X-Request-ID"), argThat(id -> {
      assertThat(id).isNotBlank();
      return true;
    }));
  }

  @Test
  void shouldClearMdcEvenIfExceptionOccurs() throws ServletException, IOException {

    when(request.getHeader("X-Request-ID")).thenReturn("error-id");
    doThrow(new RuntimeException("Filter error")).when(filterChain).doFilter(any(), any());

    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (RuntimeException ignored) {

    }

    assertThat(MDC.get("requestId")).isNull();
  }
}