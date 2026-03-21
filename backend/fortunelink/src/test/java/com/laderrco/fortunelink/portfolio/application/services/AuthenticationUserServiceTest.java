package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import com.laderrco.fortunelink.portfolio.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

@ExtendWith(MockitoExtension.class)
public class AuthenticationUserServiceTest {
  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @Mock
  private Jwt jwt;

  @InjectMocks
  private AuthenticationUserService authenticationUserService;

  @BeforeEach
  void setup() {
    // TLDR: We must set the security holder to return our mocked context
    /*
     * MORE INFO:
     * <p>
     * Normally, Mockito injects mocks into our class via constructor, but
     * with SecurityContextHolder, we use it directly, like a static call
     * we can't just pass a mock of it into the service as when running the code,
     * it will try to talke to Spring Security context (frfr). This will give
     * us a NPE because it is empty during unit testing - not integration.
     * </p>
     * <p>
     * To get around this, we are hijacking the getContext() and making
     * it return a mock context, easier for us to test.
     */
    SecurityContextHolder.setContext(securityContext);

    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(jwt);
  }

  @AfterEach
  void tearDown() {
    // Because this is static, the mock acts globally
    // so we need to clear it each time we are done
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("GetCurrentUser Tests")
  class GetCurrentUserTests {
    @Test
    void getCurrentUser_success() {
      UUID expectedId = UUID.randomUUID();
      mockAuthWithPrincipal(jwt); // Helper method to reduce boilerplate
      when(jwt.getSubject()).thenReturn(expectedId.toString());

      assertThat(authenticationUserService.getCurrentUser()).isEqualTo(expectedId);
    }

    @ParameterizedTest
    @MethodSource("provideAuthFailures")
    void getCurrentUser_failures(Object principal, Class<? extends Exception> expectedEx) {
      if (principal == null) {
        when(securityContext.getAuthentication()).thenReturn(null);
      } else {
        mockAuthWithPrincipal(principal);
      }

      assertThatThrownBy(() -> authenticationUserService.getCurrentUser())
          .isInstanceOf(expectedEx);
    }

    private static Stream<Arguments> provideAuthFailures() {
      // We need a JWT mock that specifically returns a non-UUID string
      // to trigger the IllegalArgumentException in UUID.fromString()
      Jwt mockJwtWithBadSub = mock(Jwt.class);
      when(mockJwtWithBadSub.getSubject()).thenReturn("not-a-uuid");
      return Stream.of(
          Arguments.of(null, AuthenticationException.class), // Auth is null
          Arguments.of("not-a-jwt", AuthenticationException.class), // Principal wrong type
          Arguments.of(mockJwtWithBadSub, IllegalArgumentException.class), // JWT exists but sub is invalid (null)
          Arguments.of(mock(Jwt.class), AuthenticationException.class) // Jwt exist, but missing data
      );
    }

    private void mockAuthWithPrincipal(Object principal) {
      if (principal == null) {
        // Handle the "No Auth" case
        lenient().when(securityContext.getAuthentication()).thenReturn(null);
      } else {
        // Handle the "Authenticated" case
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
      }
    }
  }

  @Nested
  @DisplayName("GetCurrentUserEmail Tests")
  class GetCurrentUserEmailTests {
    @Test
    @DisplayName("getCurrentUserEmail_success_ReturnsEmailString")
    void getCurrentUserEmail_success_WhenEmailClaimExists() {
      String expectedEmail = "user@example.com";
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(jwt);
      when(jwt.getClaim("email")).thenReturn(expectedEmail);

      String result = authenticationUserService.getCurrentUserEmail();

      assertThat(result).isEqualTo(expectedEmail);
    }

    @Test
    @DisplayName("getCurrentUserEmail_failure_NoAuth")
    void getCurrentUserEmail_failure_WhenNotAuthenticated() {
      when(securityContext.getAuthentication()).thenReturn(null);

      assertThatThrownBy(() -> authenticationUserService.getCurrentUserEmail()).isInstanceOf(
          AuthenticationException.class).hasMessage("No authenticated user found");
    }

    @Test
    @DisplayName("getCurrentUserEmail_failure_PrincipalNotJwt")
    void getCurrentUserEmail_failure_WhenPrincipalIsWrongType() {
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn("not-a-jwt"); // Wrong type

      assertThatThrownBy(() -> authenticationUserService.getCurrentUserEmail()).isInstanceOf(
          AuthenticationException.class);
    }
  }

  @Nested
  @DisplayName("VerifyOwnership Tests")
  class VerifyOwnershipTests {
    @Test
    @DisplayName("verifyUserOwnsPortfolio_success_IdsMatch")
    void verifyUserOwnsPortfolio_success_WhenUserIsOwner() {
      UserId userId = UserId.random();
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getUserId()).thenReturn(userId);

      assertThatCode(() -> authenticationUserService.verifyUserOwnsPortfolio(userId, portfolio))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyUserOwnsPortfolio_failure_IdsDoNotMatch")
    void verifyUserOwnsPortfolio_failure_WhenUserIsNotOwner() {
      UserId ownerId = UserId.random();
      UserId strangerId = UserId.random();
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getUserId()).thenReturn(ownerId);

      assertThatThrownBy(
          () -> authenticationUserService.verifyUserOwnsPortfolio(strangerId, portfolio)).isInstanceOf(
              RuntimeException.class)
          .hasMessage("Access denied: user does not own this portfolio");
    }
  }

  @Nested
  @DisplayName("IsAuthenticated Tests")
  class IsAuthenticatedTests {
    @Test
    void testIsAuthenticated() {
      // Check false
      when(securityContext.getAuthentication()).thenReturn(null);
      when(authentication.isAuthenticated()).thenReturn(false);
      assertThat(authenticationUserService.isAuthenticated()).isFalse();
      
      // Check false
      when(securityContext.getAuthentication()).thenReturn(null);
      when(authentication.isAuthenticated()).thenReturn(true);
      assertThat(authenticationUserService.isAuthenticated()).isFalse();

      // Check false
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.isAuthenticated()).thenReturn(false);
      assertThat(authenticationUserService.isAuthenticated()).isFalse();

      // Check true
      when(securityContext.getAuthentication()).thenReturn(authentication);
      when(authentication.isAuthenticated()).thenReturn(true);
      assertThat(authenticationUserService.isAuthenticated()).isTrue();
    }
  }
}
