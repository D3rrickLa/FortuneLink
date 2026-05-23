package com.laderrco.fortunelink.portfolio.api.web.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.services.UserProfileService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(UserPreferenceController.class)
@Import(AuthenticatedUserResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {
  private final UserId mockUserId = UserId.random();
  private MockMvc mockMvc;
  @Autowired
  private UserProfileController userProfileController;
  @Autowired
  private JsonMapper objectMapper;
  @MockitoBean
  private UserProfileService profileService;

  @BeforeEach
  void setUp() {
    // Build MockMvc standalone to resolve the custom @AuthenticatedUser context
    this.mockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticatedUser.class)
                && parameter.getParameterType().equals(UserId.class);
          }

          @Override
          public Object resolveArgument(MethodParameter parameter,
              ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return mockUserId;
          }
        }).build();
  }

  @Nested
  @DisplayName("GET /api/v1/users/me/profile")
  class GetProfile {

    @Test
    @DisplayName("Should return 200 and the user's full name")
    void shouldReturnUserProfile() throws Exception {
      // Arrange
      when(profileService.getFullName(any(UserId.class))).thenReturn("Jane Doe");

      // Act & Assert
      mockMvc.perform(get("/api/v1/users/me/profile").accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Jane Doe"));

      verify(profileService).getFullName(mockUserId);
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/users/me/profile")
  class UpdateProfile {

    @Test
    @DisplayName("Should return 204 and invoke service update when full name is valid")
    void shouldUpdateProfileWhenValid() throws Exception {
      // Arrange
      UserProfileController.UpdateProfileRequest validRequest = new UserProfileController.UpdateProfileRequest(
          "John Smith");

      // Act & Assert
      mockMvc.perform(put("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validRequest)))
          .andExpect(status().isNoContent());

      verify(profileService).updateFullName(eq(mockUserId), eq("John Smith"));
    }

    @Test
    @DisplayName("Should return 400 when full name is blank")
    void shouldReturn400WhenFullNameIsBlank() throws Exception {
      // Arrange - empty/blank string violates @NotBlank
      UserProfileController.UpdateProfileRequest invalidRequest = new UserProfileController.UpdateProfileRequest(
          "   ");

      // Act & Assert
      mockMvc.perform(put("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when full name exceeds 100 characters")
    void shouldReturn400WhenFullNameIsTooLong() throws Exception {
      // Arrange - 101 character string violates @Size(max = 100)
      String excessivelyLongName = "A".repeat(101);
      UserProfileController.UpdateProfileRequest invalidRequest = new UserProfileController.UpdateProfileRequest(
          excessivelyLongName);

      // Act & Assert
      mockMvc.perform(put("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());
    }
  }
}