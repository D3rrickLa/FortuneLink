package com.laderrco.fortunelink.portfolio.api.web;

import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeClosedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeReopenedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AuthorizationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidDateRangeException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // -------------------------------------------------------------------------
  // 400 Bad Request
  // -------------------------------------------------------------------------

  @ExceptionHandler(InvalidCommandException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCommand(InvalidCommandException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.withErrors("VALIDATION_ERROR", ex.getMessage(), ex.getErrors()));
  }

  @ExceptionHandler(DomainArgumentException.class)
  public ResponseEntity<ErrorResponse> handleDomainArgument(DomainArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("DOMAIN_VALIDATION_ERROR", ex.getMessage()));
  }

  // Bean validation (@Valid on @RequestBody)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).collect(Collectors.toList());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.withErrors("VALIDATION_ERROR", "Request validation failed", errors));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("BAD_REQUEST", ex.getMessage()));
  }

  // -------------------------------------------------------------------------
  // 401 Unauthorized
  // -------------------------------------------------------------------------

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of("UNAUTHORIZED", ex.getMessage()));
  }

  // -------------------------------------------------------------------------
  // 403 Forbidden
  // -------------------------------------------------------------------------

  @ExceptionHandler(AuthorizationException.class)
  public ResponseEntity<ErrorResponse> handleAuthorization(AuthorizationException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of("FORBIDDEN", ex.getMessage()));
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleSpringAccessDenied(Exception ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of("FORBIDDEN", "Access denied"));
  }

  // -------------------------------------------------------------------------
  // 404 Not Found
  // -------------------------------------------------------------------------

  @ExceptionHandler(PortfolioNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePortfolioNotFound(PortfolioNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("PORTFOLIO_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("ACCOUNT_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(TransactionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("TRANSACTION_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(AssetNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAssetNotFound(AssetNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("ASSET_NOT_FOUND", ex.getMessage()));
  }

  // -------------------------------------------------------------------------
  // 409 Conflict - state violations
  // -------------------------------------------------------------------------

  @ExceptionHandler(PortfolioLimitReachedException.class)
  public ResponseEntity<ErrorResponse> handlePortfolioLimit(PortfolioLimitReachedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("PORTFOLIO_LIMIT_REACHED", ex.getMessage()));
  }

  @ExceptionHandler(PortfolioNotEmptyException.class)
  public ResponseEntity<ErrorResponse> handlePortfolioNotEmpty(PortfolioNotEmptyException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("PORTFOLIO_NOT_EMPTY", ex.getMessage()));
  }

  @ExceptionHandler(PortfolioDeletionException.class)
  public ResponseEntity<ErrorResponse> handlePortfolioDeletion(PortfolioDeletionException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("PORTFOLIO_DELETION_ERROR", ex.getMessage()));
  }

  @ExceptionHandler(AccountCannotBeClosedException.class)
  public ResponseEntity<ErrorResponse> handleAccountCannotBeClosed(
      AccountCannotBeClosedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("ACCOUNT_CANNOT_BE_CLOSED", ex.getMessage()));
  }

  @ExceptionHandler(AccountCannotBeReopenedException.class)
  public ResponseEntity<ErrorResponse> handleAccountCannotBeReopened(
      AccountCannotBeReopenedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("ACCOUNT_CANNOT_BE_REOPENED", ex.getMessage()));
  }

  @ExceptionHandler(AccountClosedException.class)
  public ResponseEntity<ErrorResponse> handleAccountClosed(AccountClosedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("ACCOUNT_CLOSED", ex.getMessage()));
  }

  @ExceptionHandler(InvalidTransactionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTransaction(InvalidTransactionException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("INVALID_TRANSACTION_STATE", ex.getMessage()));
  }

  @ExceptionHandler(InvalidDateRangeException.class)
  public ResponseEntity<ErrorResponse> handleInvalidDateRange(InvalidDateRangeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("INVALID_DATE_RANGE", ex.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("CONFLICT", ex.getMessage()));
  }

  // -------------------------------------------------------------------------
  // 422 Unprocessable - business rule violations with valid data
  // -------------------------------------------------------------------------

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse.of("INSUFFICIENT_FUNDS", ex.getMessage()));
  }

  @ExceptionHandler(InsufficientQuantityException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientQuantity(
      InsufficientQuantityException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse.of("INSUFFICIENT_QUANTITY", ex.getMessage()));
  }

  @ExceptionHandler(CurrencyMismatchException.class)
  public ResponseEntity<ErrorResponse> handleCurrencyMismatch(CurrencyMismatchException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse.of("CURRENCY_MISMATCH", ex.getMessage()));
  }

  // -------------------------------------------------------------------------
  // ResponseStatusException - from controllers that throw it directly
  // -------------------------------------------------------------------------

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(ErrorResponse.of("REQUEST_ERROR", ex.getReason()));
  }

  // -------------------------------------------------------------------------
  // 500 catch-all - last resort
  // -------------------------------------------------------------------------

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    // Log at ERROR with full stack trace, this should never happen in production
    // If it does, there's a missing handler above
    log.error("Unhandled exception: add a specific handler for: {}", ex.getClass().getName(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred"));
  }

  // -------------------------------------------------------------------------
  // Error response shape
  // -------------------------------------------------------------------------

  public record ErrorResponse(
      String code, String message, List<String> errors, Instant timestamp) {

    public static ErrorResponse of(String code, String message) {
      return new ErrorResponse(code, message, List.of(), Instant.now());
    }

    public static ErrorResponse withErrors(String code, String message, List<String> errors) {
      return new ErrorResponse(code, message, errors, Instant.now());
    }
  }
}
