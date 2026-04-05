package com.laderrco.fortunelink.portfolio.api.web;

import com.laderrco.fortunelink.portfolio.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AuthorizationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(PortfolioNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePortfolioNotFound(PortfolioNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("PORTFOLIO_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(AuthorizationException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationException(AuthorizationException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of("FORBIDDEN", ex.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of("UNAUTHORIZED", ex.getMessage()));
  }

  @ExceptionHandler(InvalidCommandException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCommand(InvalidCommandException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("VALIDATION_ERROR", ex.getMessage()));
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse.of("INSUFFICIENT_FUNDS", ex.getMessage()));
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("ACCOUNT_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred"));
  }

  public record ErrorResponse(String code, String message, Instant timestamp) {
    public static ErrorResponse of(String code, String message) {
      return new ErrorResponse(code, message, Instant.now());
    }
  }
}