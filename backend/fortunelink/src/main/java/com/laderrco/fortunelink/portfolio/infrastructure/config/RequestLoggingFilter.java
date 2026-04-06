package com.laderrco.fortunelink.portfolio.infrastructure.config;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// IF a bug is filed, the transaction will disappear, no way
// of knowing what happen without this filter
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
      FilterChain chain) throws IOException, ServletException {
    String requestId = Optional.ofNullable(req.getHeader("X-Request-ID"))
        .orElse(UUID.randomUUID().toString());
    MDC.put("requestId", requestId);
    res.setHeader("X-Request-ID", requestId);
    try {
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
    }
  }
}