package com.laderrco.fortunelink.portfolio.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioLifecycleService;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.SecurityTestsSuite.TestSecurityConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(useDefaultFilters = false) // Stops it from scanning all controllers
@Import({ TestSecurityConfig.class,
    com.laderrco.fortunelink.portfolio.infrastructure.config.SecurityConfigTest.DummyController.class })
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PortfolioQueryService queryService;
  @MockitoBean
  private PortfolioLifecycleService lifecycleService;
  @MockitoBean
  private AuthenticationUserService authService;

  @Test
  void publicEndpointsShouldBeAccessibleWithoutToken() throws Exception {
    // Now hitting the actual path mapped in our DummyController
    mockMvc.perform(get("/api/v1/public/test"))
        .andExpect(status().isOk());
  }

  @Test
  void privateEndpointsShouldReturn401WhenUnauthenticated() throws Exception {
    // Hitting the private path without security context
    mockMvc.perform(get("/api/v1/private/test"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void privateEndpointsShouldReturn200WhenAuthenticated() throws Exception {
    // Hitting the private path WITH a mock JWT
    mockMvc.perform(get("/api/v1/private/test")
        .with(jwt().jwt(j -> j.subject("test-user"))))
        .andExpect(status().isOk());
  }

  // A tiny dummy controller just to give MockMvc something to hit
  @RestController
  static class DummyController {
    @GetMapping("/api/v1/public/test")
    public void publicTest() {
    }

    @GetMapping("/api/v1/private/test")
    public void privateTest() {
    }
  }

}