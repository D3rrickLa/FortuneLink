package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.application.commands.UpdateUserPreferencesCommand;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.UserPreferencesService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(UserPreferenceController.class)
@Import(AuthenticatedUserResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class UserPreferenceControllerTest {

  // Helper to simulate your custom @AuthenticatedUser argument resolver behavior if needed.
  // If your security setup automatically populates this, make sure your test configuration
  // mock-injects a UserId or handles the custom annotation resolver.
  private final UserId mockUserId = UserId.random();
  @Autowired
  MockMvc mockMvc;
  @Autowired
  JsonMapper objectMapper;
  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;
  @MockitoBean
  private UserPreferencesService preferencesService;

  @Nested
  @DisplayName("GET /api/v1/users/me/preferences")
  class GetPreferences {

    @Test
    @DisplayName("Should return 200 and preferences when they exist")
    void shouldReturnPreferences() throws Exception {
      // Arrange
      UserPreferences mockPreferences = new UserPreferences(mockUserId, Currency.of("CAD"), true,
          false, "YYYY-MM-DD");
      when(preferencesService.get(any(UserId.class))).thenReturn(mockPreferences);

      // Act & Assert
      mockMvc.perform(get("/api/v1/users/me/preferences").contentType(MediaType.APPLICATION_JSON)
              .content(mockUserId.id().toString())
              .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
          .andExpect(jsonPath("$.baseCurrency").value("CAD"))
          .andExpect(jsonPath("$.emailNotifications").value(true))
          .andExpect(jsonPath("$.priceAlerts").value(false))
          .andExpect(jsonPath("$.dateFormat").value("YYYY-MM-DD"));
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/users/me/preferences")
  class UpdatePreferences {

    @Test
    @DisplayName("Should return 204 and trigger service update when request is valid")
    void shouldUpdatePreferencesWhenValid() throws Exception {
      // Arrange
      UserPreferenceController.UpdateUserPreferencesRequest validRequest = new UserPreferenceController.UpdateUserPreferencesRequest(
          "USD", true, true, "MM/DD/YYYY");

      // Act & Assert
      mockMvc.perform(put("/api/v1/users/me/preferences").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validRequest)))
          .andExpect(status().isNoContent());

      UpdateUserPreferencesCommand expectedCommand = new UpdateUserPreferencesCommand(
          Currency.of("USD"), true, true, "MM/DD/YYYY");
      verify(preferencesService).updatePreferences(any(UserId.class), eq(expectedCommand));
    }

    @Test
    @DisplayName("Should return 400 when currency code is lowercase or wrong length")
    void shouldReturn400WhenCurrencyIsInvalid() throws Exception {
      // Arrange - lowercase and 4 characters violates @Pattern and @Size
      UserPreferenceController.UpdateUserPreferencesRequest invalidRequest = new UserPreferenceController.UpdateUserPreferencesRequest(
          "usda", true, true, "YYYY-MM-DD");

      // Act & Assert
      mockMvc.perform(put("/api/v1/users/me/preferences").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when date format is blank")
    void shouldReturn400WhenDateFormatIsBlank() throws Exception {
      // Arrange - blank date format violates @NotBlank
      UserPreferenceController.UpdateUserPreferencesRequest invalidRequest = new UserPreferenceController.UpdateUserPreferencesRequest(
          "USD", true, true, "   ");

      // Act & Assert
      mockMvc.perform(put("/api/v1/users/me/preferences").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());
    }
  }
}