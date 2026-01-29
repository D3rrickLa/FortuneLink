package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.AuthenticationException;

// any changes to the auth provider, we can change this
@Service
public class AuthenticationUserService {

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new AuthenticationException("No authenticated user");
    }

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("email");
        }
        throw new AuthenticationException("No authenticated user found");
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
