package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationUserServiceTest {

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private AuthenticationUserService authService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setup() {
        // We must mock the static Holder to return our mocked Context
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }

    @Nested
    @DisplayName("GetCurrentUser Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("getCurrentUser_Success_ReturnsUuidFromSub")
        void getCurrentUser_Success_ValidSupabaseSub() {
            UUID expectedUuid = UUID.randomUUID();
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(jwt);
            when(jwt.getSubject()).thenReturn(expectedUuid.toString());

            UUID result = authService.getCurrentUser();

            assertThat(result).isEqualTo(expectedUuid);
        }

        @Test
        @DisplayName("getCurrentUser_Success_ValidJwt")
        void getCurrentUser_Success_WhenAllConditionsMet() {
            // Branch: both conditions TRUE
            UUID expectedId = UUID.randomUUID();
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(jwt);
            when(jwt.getSubject()).thenReturn(expectedId.toString());

            UUID result = authService.getCurrentUser();

            assertThat(result).isEqualTo(expectedId);
        }

        @Test
        @DisplayName("getCurrentUser_Failure_InvalidUuidFormat")
        void getCurrentUser_Failure_WhenSubIsNotUUID() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(jwt);
            when(jwt.getSubject()).thenReturn("not-a-uuid");

            assertThatThrownBy(() -> authService.getCurrentUser())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getCurrentUser_Failure_AuthIsNull")
        void getCurrentUser_Failure_WhenAuthenticationIsNull() {
            // Branch: auth != null is FALSE
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThatThrownBy(() -> authService.getCurrentUser())
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("No authenticated user");
        }

        @Test
        @DisplayName("getCurrentUser_Failure_PrincipalNotJwt")
        void getCurrentUser_Failure_WhenPrincipalIsWrongType() {
            // Branch: auth != null is TRUE, but instanceof Jwt is FALSE
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn("I am a String, not a Jwt");

            assertThatThrownBy(() -> authService.getCurrentUser())
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("No authenticated user");
        }

    }

    @Nested
    @DisplayName("GetCurrentUserEmail Tests")
    class GetCurrentUserEmailTests {

        @Test
        @DisplayName("getCurrentUserEmail_Success_ReturnsEmailString")
        void getCurrentUserEmail_Success_WhenEmailClaimExists() {
            String expectedEmail = "user@example.com";
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(jwt);
            when(jwt.getClaim("email")).thenReturn(expectedEmail);

            String result = authService.getCurrentUserEmail();

            assertThat(result).isEqualTo(expectedEmail);
        }

        @Test
        @DisplayName("getCurrentUserEmail_Failure_NoAuth")
        void getCurrentUserEmail_Failure_WhenNotAuthenticated() {
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThatThrownBy(() -> authService.getCurrentUserEmail())
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("No authenticated user found");
        }

        @Test
        @DisplayName("getCurrentUserEmail_Failure_PrincipalNotJwt")
        void getCurrentUserEmail_Failure_WhenPrincipalIsWrongType() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn("not-a-jwt"); // Wrong type

            assertThatThrownBy(() -> authService.getCurrentUserEmail())
                    .isInstanceOf(AuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("VerifyOwnership Tests")
    class VerifyOwnershipTests {

        @Test
        @DisplayName("verifyUserOwnsPortfolio_Success_IdsMatch")
        void verifyUserOwnsPortfolio_Success_WhenUserIsOwner() {
            UserId userId = UserId.random();
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolio.getUserId()).thenReturn(userId);

            assertThatCode(() -> authService.verifyUserOwnsPortfolio(userId, portfolio))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("verifyUserOwnsPortfolio_Failure_IdsDoNotMatch")
        void verifyUserOwnsPortfolio_Failure_WhenUserIsNotOwner() {
            UserId ownerId = UserId.random();
            UserId strangerId = UserId.random();
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolio.getUserId()).thenReturn(ownerId);

            assertThatThrownBy(() -> authService.verifyUserOwnsPortfolio(strangerId, portfolio))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Access denied: user does not own this portfolio");
        }
    }

    @Nested
    @DisplayName("IsAuthenticated Tests")
    class IsAuthenticatedTests {

        @Test
        @DisplayName("isAuthenticated_Success_True")
        void isAuthenticated_Success_ReturnsTrue() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);

            assertThat(authService.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("isAuthenticated_Success_False")
        void isAuthenticated_Success_ReturnsFalseWhenNull() {
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThat(authService.isAuthenticated()).isFalse();
        }
    }
}