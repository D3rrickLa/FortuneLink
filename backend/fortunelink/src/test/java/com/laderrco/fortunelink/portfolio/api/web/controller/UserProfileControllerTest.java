package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.UserProfileService;
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

@WebMvcTest(controllers = UserProfileController.class)
@Import(AuthenticatedUserResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private static final String BASE_URL = "/api/v1/users/me/profile";

  @Autowired
  MockMvc mockMvc;

  @Autowired
  JsonMapper objectMapper;

  @MockitoBean
  UserProfileService profileService;

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
  void getProfile_returnsUserProfile() throws Exception {
    when(profileService.getFullName(new UserId(USER_UUID))).thenReturn("Jane Doe");

    mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Jane Doe"));

    verify(profileService).getFullName(new UserId(USER_UUID));
  }

  @Test
  void updateProfile_updatesFullName() throws Exception {
    var request = new UserProfileController.UpdateProfileRequest("Jane Doe");

    doNothing().when(profileService).updateFullName(new UserId(USER_UUID), "Jane Doe");

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isNoContent());

    verify(profileService).updateFullName(new UserId(USER_UUID), "Jane Doe");
  }

  @Test
  void updateProfile_returnsBadRequest_whenFullNameIsBlank() throws Exception {

    var request = new UserProfileController.UpdateProfileRequest("");

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void updateProfile_returnsBadRequest_whenFullNameTooLong() throws Exception {

    String longName = "a".repeat(101);

    var request = new UserProfileController.UpdateProfileRequest(longName);

    mockMvc.perform(put(BASE_URL).contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }
}