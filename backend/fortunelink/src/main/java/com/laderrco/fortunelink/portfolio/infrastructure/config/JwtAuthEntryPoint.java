package com.laderrco.fortunelink.portfolio.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
  @Override
  public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
      throws IOException {
    res.setContentType("application/json");
    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    res.getWriter().write("""
        {"code":"UNAUTHORIZED","message":"Authentication required","timestamp":"%s"}
        """.formatted(Instant.now()));
  }
}