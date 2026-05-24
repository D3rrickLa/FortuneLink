package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = UserPreferenceController.class)
@Import(AuthenticatedUserResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class UserPreferenceControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final String BASE_URL = "/api/v1/users/me/preferences";

  @Autowired
  MockMvc mockMvc;

  @Autowired
  JsonMapper objectMapper;

  @MockitoBean
  UserPreferencesService preferencesService;

  // Backing bean for AuthenticatedUserResolver
  @MockitoBean
  AuthenticationUserService authenticationUserService;

  // WebConfig dependency
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {

    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);

    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);
  }

  @Test
  void getPreferences_returnsUserPreferences() throws Exception {

    UserPreferences preferences = UserPreferences.builder().baseCurrency(Currency.of("CAD"))
        .emailNotifications(true).priceAlerts(true).dateFormat("YYYY-MM-DD").build();

    when(preferencesService.get(new UserId(USER_UUID))).thenReturn(preferences);

    mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
        .andExpect(jsonPath("$.baseCurrency").value("CAD"))
        .andExpect(jsonPath("$.emailNotifications").value(true))
        .andExpect(jsonPath("$.priceAlerts").value(true))
        .andExpect(jsonPath("$.dateFormat").value("YYYY-MM-DD"));

    verify(preferencesService).get(new UserId(USER_UUID));
  }

  @Test
  void updatePreferences_updatesUserPreferences() throws Exception {

    var request = new UserPreferenceController.UpdateUserPreferencesRequest("USD", true, false,
        "YYYY-MM-DD");

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isNoContent());

    verify(preferencesService).updatePreferences(new UserId(USER_UUID),
        new UpdateUserPreferencesCommand(Currency.of("USD"), true, false, "YYYY-MM-DD"));
  }

  @Test
  void updatePreferences_returnsBadRequest_whenCurrencyIsBlank() throws Exception {

    var request = new UserPreferenceController.UpdateUserPreferencesRequest("", true, true,
        "YYYY-MM-DD");

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void updatePreferences_returnsBadRequest_whenCurrencyInvalid() throws Exception {

    var request = new UserPreferenceController.UpdateUserPreferencesRequest("usd", true, true,
        "YYYY-MM-DD");

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void updatePreferences_returnsBadRequest_whenCurrencyTooLong() throws Exception {

    var request = new UserPreferenceController.UpdateUserPreferencesRequest("USDD", true, true,
        "YYYY-MM-DD");

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void updatePreferences_returnsBadRequest_whenDateFormatBlank() throws Exception {

    var request = new UserPreferenceController.UpdateUserPreferencesRequest("USD", true, true, "");

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }
}