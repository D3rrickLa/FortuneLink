package com.laderrco.fortunelink.portfolio.infrastructure.config;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
Gorrelate the logs across the controller, services, and repositories + providers
*/
@Component
public class CorrelationIdFilter implements Filter {
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    String correlationId = Optional.ofNullable(
        ((HttpServletRequest) req).getHeader("X-Request-ID"))
        .orElse(UUID.randomUUID().toString());
    MDC.put("correlationId", correlationId);
    ((HttpServletResponse) res).addHeader("X-Request-ID", correlationId);
    try {
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
    }
  }
}