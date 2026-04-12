package com.laderrco.fortunelink.portfolio.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.laderrco.fortunelink.portfolio.application.services.AccountLifecycleService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest
@Import(WebConfig.class)
@ActiveProfiles("test")
class WebConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockitoBean
  private AuthenticatedUserResolver authenticatedUserResolver;

  @MockitoBean
  private JwtDecoder jwtDecoder;
  @MockitoBean
  private JwtAuthEntryPoint jwtAuthEntryPoint;
  @MockitoBean
  private AccountLifecycleService accountLifecycleService;

  @Test
  void shouldRegisterRateLimitInterceptor() throws Exception {
    when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    mockMvc.perform(get("/api/test")).andExpect(status().isUnauthorized()); 
    verify(rateLimitInterceptor, atLeastOnce()).preHandle(any(), any(), any());
  }

  @Test
  void shouldExcludeActuatorFromInterceptors() throws Exception {
    mockMvc.perform(get("/actuator/health"));

    verifyNoInteractions(rateLimitInterceptor);
  }
}