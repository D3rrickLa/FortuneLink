package com.laderrco.fortunelink.portfolio.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthEntryPointTest {

  private JwtAuthEntryPoint authEntryPoint;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private StringWriter stringWriter;

  @BeforeEach
  void setUp() throws IOException {
    authEntryPoint = new JwtAuthEntryPoint();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);

    
    stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
  }

  @Test
  void shouldReturnUnauthorizedJsonOnCommence() throws IOException {
    
    AuthenticationException authEx = mock(AuthenticationException.class);

    
    authEntryPoint.commence(request, response, authEx);

    
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("application/json");

    
    String output = stringWriter.toString();
    assertThat(output).contains("\"code\":\"UNAUTHORIZED\"");
    assertThat(output).contains("\"message\":\"Authentication required\"");
    assertThat(output).contains("\"timestamp\":");
  }
}