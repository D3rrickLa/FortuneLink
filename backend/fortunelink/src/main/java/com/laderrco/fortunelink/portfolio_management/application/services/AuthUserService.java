package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

@Service
public class AuthUserService {
    UUID getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new AuthenticationException("No authenticated user");
    }

    UUID getUserById(UserId userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("email");
        }
        throw new AuthenticationException("No authenticated user found");
    }

    void verifyUserOwnsPortfolio(UserId userId, Portfolio portfolio) {
        if (!userId.equals(portfolio.getUserId())) {
            throw new AuthenticationException("No authenticated user found");
        }
    }
}
