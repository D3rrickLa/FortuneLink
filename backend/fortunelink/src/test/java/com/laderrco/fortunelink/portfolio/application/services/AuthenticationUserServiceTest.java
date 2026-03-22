package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication User Service Tests")
class AuthenticationUserServiceTest {
  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @Mock
  private Jwt jwt;

  @InjectMocks
  private AuthenticationUserService authenticationUserService;

  @BeforeEach
  void setUp() {
    /*
     * We hijack the static SecurityContextHolder.getContext() by setting
     * it to our mock context. This prevents NullPointerExceptions that
     * occur when the service tries to talk to an empty Spring Security context.
     */
    SecurityContextHolder.setContext(securityContext);

    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(jwt);
  }

  @AfterEach
  void tearDown() {
    // Clear global static state to prevent test pollution
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("getCurrentUser() Operations")
  class GetCurrentUserTests {

    private static Stream<Arguments> provideAuthFailures() {
      Jwt mockJwtWithBadSub = mock(Jwt.class);
      when(mockJwtWithBadSub.getSubject()).thenReturn("not-a-uuid");

      return Stream.of(
          Arguments.of(null, AuthenticationException.class), // Auth is null
          Arguments.of("not-a-jwt", AuthenticationException.class), // Principal wrong type
          Arguments.of(mockJwtWithBadSub, IllegalArgumentException.class), // Invalid UUID string
          Arguments.of(mock(Jwt.class), AuthenticationException.class) // Missing data
      );
    }

    @Test
    @DisplayName("getCurrentUser: returns UUID when JWT subject is valid")
    void getCurrentUserValidSubjectReturnsUuid() {
      UUID expectedId = UUID.randomUUID();
      mockAuthWithPrincipal(jwt);
      when(jwt.getSubject()).thenReturn(expectedId.toString());

      assertThat(authenticationUserService.getCurrentUser()).isEqualTo(expectedId);
    }

    @ParameterizedTest
    @MethodSource("provideAuthFailures")
    @DisplayName("getCurrentUser: throws expected exception on auth failures")
    void getCurrentUserAuthFailuresThrowCorrectException(Object principal, Class<? extends Exception> expectedEx) {
      if (principal == null) {
        when(securityContext.getAuthentication()).thenReturn(null);
      } else {
        mockAuthWithPrincipal(principal);
      }

      assertThatThrownBy(() -> authenticationUserService.getCurrentUser()).isInstanceOf(expectedEx);
    }

    private void mockAuthWithPrincipal(Object principal) {
      if (principal == null) {
        lenient().when(securityContext.getAuthentication()).thenReturn(null);
      } else {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
      }
    }
  }

  @Nested
  @DisplayName("getCurrentUserEmail() Operations")
  class GetCurrentUserEmailTests {

    @Test
    @DisplayName("getCurrentUserEmail: returns email claim from JWT")
    void getCurrentUserEmailSuccessWhenClaimExists() {
      String expectedEmail = "user@example.com";
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(jwt);
      when(jwt.getClaim("email")).thenReturn(expectedEmail);

      assertThat(authenticationUserService.getCurrentUserEmail()).isEqualTo(expectedEmail);
    }

    @Test
    @DisplayName("getCurrentUserEmail: throws exception when not authenticated")
    void getCurrentUserEmailFailureWhenNotAuthenticated() {
      when(securityContext.getAuthentication()).thenReturn(null);

      assertThatThrownBy(() -> authenticationUserService.getCurrentUserEmail())
          .isInstanceOf(AuthenticationException.class)
          .hasMessage("No authenticated user found");
    }

    @Test
    @DisplayName("getCurrentUserEmail: throws exception when principal is wrong type")
    void getCurrentUserEmailFailureWhenPrincipalIsWrongType() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn("not-a-jwt");

      assertThatThrownBy(() -> authenticationUserService.getCurrentUserEmail())
          .isInstanceOf(AuthenticationException.class);
    }
  }

  @Nested
  @DisplayName("verifyOwnership() Operations")
  class VerifyOwnershipTests {

    @Test
    @DisplayName("verifyUserOwnsPortfolio: allows access when IDs match")
    void verifyUserOwnsPortfolioSuccessWhenIdsMatch() {
      UserId userId = UserId.random();
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getUserId()).thenReturn(userId);

      assertThatCode(() -> authenticationUserService.verifyUserOwnsPortfolio(userId, portfolio))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyUserOwnsPortfolio: denies access when IDs do not match")
    void verifyUserOwnsPortfolioFailureWhenIdsDoNotMatch() {
      UserId ownerId = UserId.random();
      UserId strangerId = UserId.random();
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getUserId()).thenReturn(ownerId);

      assertThatThrownBy(() -> authenticationUserService.verifyUserOwnsPortfolio(strangerId, portfolio))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Access denied: user does not own this portfolio");
    }
  }

  @Nested
  @DisplayName("isAuthenticated() Operations")
  class IsAuthenticatedTests {

    @Test
    @DisplayName("isAuthenticated: returns false if authentication object is missing")
    void isAuthenticatedFalseWhenAuthMissing() {
      when(securityContext.getAuthentication()).thenReturn(null);
      assertThat(authenticationUserService.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticated: returns false if authentication object says it is not authenticated")
    void isAuthenticatedFalseWhenAuthSaysFalse() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.isAuthenticated()).thenReturn(false);
      assertThat(authenticationUserService.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticated: returns true only if auth exists and is flagged as true")
    void isAuthenticatedTrueWhenAuthExistsAndIsTrue() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.isAuthenticated()).thenReturn(true);
      assertThat(authenticationUserService.isAuthenticated()).isTrue();
    }
  }
}