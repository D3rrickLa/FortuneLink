package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AuthorizationException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationUserService {

  public UUID getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      // Supabase stores the User UUID in the "sub" claim
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

  public void verifyUserOwnsPortfolio(UserId userId, Portfolio portfolio) {
    if (!userId.equals(portfolio.getUserId())) {
      throw new AuthorizationException("Access denied: user does not own this portfolio");
    }
  }

  public boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.isAuthenticated();
  }
}
