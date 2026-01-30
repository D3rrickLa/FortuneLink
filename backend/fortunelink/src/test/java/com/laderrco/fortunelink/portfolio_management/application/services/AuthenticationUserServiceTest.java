package com.laderrco.fortunelink.portfolio_management.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.AuthenticationException;

class AuthenticationUserServiceTest {

    private AuthenticationUserService authService;
    private SecurityContext securityContext;
    private Authentication mockAuth;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationUserService();
        securityContext = mock(SecurityContext.class);
        mockAuth = mock(Authentication.class);
        mockJwt = mock(Jwt.class);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return User ID and Email when valid JWT exists")
    void successPath_ValidJwt() {
        UUID expectedUuid = UUID.randomUUID();
        String expectedEmail = "user@example.com";

        when(securityContext.getAuthentication()).thenReturn(mockAuth);
        when(mockAuth.getPrincipal()).thenReturn(mockJwt);
        when(mockJwt.getSubject()).thenReturn(expectedUuid.toString());
        when(mockJwt.getClaim("email")).thenReturn(expectedEmail);

        assertEquals(expectedUuid, authService.getCurrentUserId());
        assertEquals(expectedEmail, authService.getCurrentUserEmail());
    }

    @Test
    @DisplayName("Should throw AuthenticationException when SecurityContext is empty")
    void failurePath_NoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(AuthenticationException.class, () -> authService.getCurrentUserId());
        assertThrows(AuthenticationException.class, () -> authService.getCurrentUserEmail());
    }

    @Test
    @DisplayName("Should throw AuthenticationException when Principal is null or wrong type")
    void failurePath_InvalidPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(mockAuth);

        // Branch: Principal is null
        when(mockAuth.getPrincipal()).thenReturn(null);
        assertThrows(AuthenticationException.class, () -> authService.getCurrentUserId());

        // Branch: Principal is wrong type (String)
        when(mockAuth.getPrincipal()).thenReturn("not-a-jwt");
        assertThrows(AuthenticationException.class, () -> authService.getCurrentUserId());

        // Branch: Principal is wrong type (Object) - covers specific pattern match
        // failure
        when(mockAuth.getPrincipal()).thenReturn(new Object());
        assertThrows(AuthenticationException.class, () -> authService.getCurrentUserEmail());
    }

    @Test
    @DisplayName("Should correctly report authentication status")
    void authenticationStatus_Branches() {
        // Case: Null Auth
        when(securityContext.getAuthentication()).thenReturn(null);
        assertFalse(authService.isAuthenticated());

        // Case: Auth present but isAuthenticated() is false
        when(securityContext.getAuthentication()).thenReturn(mockAuth);
        when(mockAuth.isAuthenticated()).thenReturn(false);
        assertFalse(authService.isAuthenticated());

        // Case: Auth present and isAuthenticated() is true
        when(mockAuth.isAuthenticated()).thenReturn(true);
        assertTrue(authService.isAuthenticated());
    }
}