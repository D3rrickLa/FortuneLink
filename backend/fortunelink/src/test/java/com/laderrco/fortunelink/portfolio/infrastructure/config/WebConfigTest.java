package com.laderrco.fortunelink.portfolio.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = TestController.class)
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

  @BeforeEach
  void setUp() {
    
    when(authenticatedUserResolver.supportsParameter(any())).thenReturn(false);
  }

  @Test
  void shouldRegisterRateLimitInterceptor() throws Exception {
    when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    mockMvc.perform(get("/api/test").with(user("test")))
        .andExpect(status().isOk()); 

    verify(rateLimitInterceptor, atLeastOnce()).preHandle(any(), any(), any());
  }

}

@RestController
class TestController {
  @GetMapping("/api/test")
  public String test() {
    return "success";
  }

  @GetMapping("/actuator/health")
  public void health() {
  }
}