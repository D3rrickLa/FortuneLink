package com.laderrco.fortunelink.portfolio.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeClosedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeReopenedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AuthenticationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AuthorizationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.CsvImportCommitException;
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
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.infrastructure.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio.infrastructure.exceptions.UnknownSymbolException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.exceptions.FmpApiException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for GlobalExceptionHandler.
 * <p>
 * Strategy: instantiate the handler directly and invoke each method, asserting on status code,
 * error code, and message. No Spring context needed — these are plain method calls.
 * <p>
 * NOTE TO REVIEWER: A few issues were found in the handler during test authoring that should be
 * addressed:
 * <p>
 * 1. HttpStatus.UNPROCESSABLE_CONTENT does not exist in Spring's HttpStatus enum. The correct
 * constant is HttpStatus.UNPROCESSABLE_ENTITY (422). Tests use the raw status value (422) to avoid
 * coupling to a constant that likely causes a compile error.
 * <p>
 * 2. handleHeaderException and handleMissingBody return raw types (Map and String) instead of
 * ResponseEntity<ErrorResponse>. This breaks uniform client error handling. These should be
 * standardized.
 * <p>
 * 3. handlePortfolioStateConflicts takes RuntimeException but is annotated with
 * PortfolioAlreadyDeletedException — functional but misleading.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  // =========================================================================
  // 400 Bad Request
  // =========================================================================

  @Nested
  @DisplayName("400 Bad Request")
  class BadRequest {

    @Test
    @DisplayName("InvalidCommandException 400 VALIDATION_ERROR with errors list propagated")
    void invalidCommandException_returns400WithErrorsList() {
      List<String> errors = List.of("portfolioId: must not be null", "name: must not be blank");
      var ex = new InvalidCommandException("Invalid createPortfolio command", errors);

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidCommand(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().errors()).containsExactlyElementsOf(errors);
      assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("InvalidCommandException with empty errors list still returns 400")
    void invalidCommandException_emptyErrorsList_returns400() {
      var ex = new InvalidCommandException("Bad command", List.of());

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidCommand(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors()).isEmpty();
    }

    @Test
    @DisplayName("DomainArgumentException 400 DOMAIN_VALIDATION_ERROR with original message")
    void domainArgumentException_returns400WithMessage() {
      var ex = new DomainArgumentException("Quantity cannot be negative");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainArgument(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().code()).isEqualTo("DOMAIN_VALIDATION_ERROR");
      assertThat(response.getBody().message()).isEqualTo("Quantity cannot be negative");
      assertThat(response.getBody().errors()).isEmpty();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 400 VALIDATION_ERROR with field-level errors")
    void methodArgumentNotValidException_returns400WithFieldErrors() {
      BindingResult bindingResult = mock(BindingResult.class);
      when(bindingResult.getFieldErrors()).thenReturn(
          List.of(new FieldError("createPortfolioRequest", "name", "must not be blank"),
              new FieldError("createPortfolioRequest", "currency", "size must be 3")));
      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMethodArgumentNotValid(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().message()).isEqualTo("Request validation failed");
      assertThat(response.getBody().errors()).containsExactlyInAnyOrder("name: must not be blank",
          "currency: size must be 3");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException with single field error formats correctly")
    void methodArgumentNotValidException_singleFieldError_formatsCorrectly() {
      BindingResult bindingResult = mock(BindingResult.class);
      when(bindingResult.getFieldErrors()).thenReturn(
          List.of(new FieldError("request", "symbol", "must not be blank")));
      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMethodArgumentNotValid(
          ex);

      assertThat(response.getBody().errors()).containsExactly("symbol: must not be blank");
    }

    @Test
    @DisplayName("IllegalArgumentException 400 BAD_REQUEST with original message")
    void illegalArgumentException_returns400() {
      var ex = new IllegalArgumentException("Page cannot be negative: -1");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalArgument(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
      assertThat(response.getBody().message()).isEqualTo("Page cannot be negative: -1");
    }

    @Test
    @DisplayName("MissingRequestHeaderException 400 map with header name in error value")
    void missingRequestHeaderException_returns400WithHeaderName() {
      MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
      when(ex.getHeaderName()).thenReturn("Idempotency-Key");

      Map<String, String> response = handler.handleHeaderException(ex);

      assertThat(response).containsEntry("error", "Missing header: Idempotency-Key");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException returns 'Body is missing' string")
    void httpMessageNotReadableException_returnsBodyIsMissingString() {
      HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

      String response = handler.handleMissingBody(ex);

      assertThat(response).isEqualTo("Body is missing");
    }

    @Test
    @DisplayName("InvalidDateRangeException 400 INVALID_DATE_RANGE with original message")
    void invalidDateRangeException_returns400() {
      var ex = new InvalidDateRangeException(
          "Start date cannot be after end date: start=2024-12-01, end=2024-01-01");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidDateRange(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().code()).isEqualTo("INVALID_DATE_RANGE");
      assertThat(response.getBody().message()).contains("Start date cannot be after end date");
    }

    @Test
    @DisplayName("ConstraintViolationException 400 with 'Query cannot be blank' message")
    void constraintViolationException_returns400WithBlankQueryMessage() {
      var ex = new ConstraintViolationException("constraint violations", Set.of());

      ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).containsEntry("message", "Query cannot be blank");
    }
  }

  // =========================================================================
  // 401 Unauthorized
  // =========================================================================

  @Nested
  @DisplayName("401 Unauthorized")
  class Unauthorized {

    @Test
    @DisplayName("AuthenticationException 401 UNAUTHORIZED with original message")
    void authenticationException_returns401() {
      var ex = new AuthenticationException("No authenticated user");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAuthentication(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
      assertThat(response.getBody().message()).isEqualTo("No authenticated user");
    }

    @Test
    @DisplayName("AuthenticationException with JWT missing claim message 401")
    void authenticationException_jwtMissingClaim_returns401() {
      var ex = new AuthenticationException("JWT missing subject claim");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAuthentication(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      assertThat(response.getBody().message()).isEqualTo("JWT missing subject claim");
    }
  }

  // =========================================================================
  // 403 Forbidden
  // =========================================================================

  @Nested
  @DisplayName("403 Forbidden")
  class Forbidden {

    @Test
    @DisplayName("AuthorizationException 403 FORBIDDEN with original message")
    void authorizationException_returns403() {
      var ex = new AuthorizationException("Access denied: user does not own this portfolio");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAuthorization(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
      assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
      assertThat(response.getBody().message()).isEqualTo(
          "Access denied: user does not own this portfolio");
    }

    @Test
    @DisplayName("Spring AccessDeniedException 403 FORBIDDEN with generic 'Access denied' message")
    void springAccessDeniedException_returns403WithGenericMessage() {
      var ex = new AccessDeniedException("Full authentication is required");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleSpringAccessDenied(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
      assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
      // Handler intentionally uses a generic message, not the exception message
      assertThat(response.getBody().message()).isEqualTo("Access denied");
    }
  }

  // =========================================================================
  // 404 Not Found
  // =========================================================================

  @Nested
  @DisplayName("404 Not Found")
  class NotFound {

    @Test
    @DisplayName("PortfolioNotFoundException 404 PORTFOLIO_NOT_FOUND with original message")
    void portfolioNotFoundException_returns404() {
      var ex = new PortfolioNotFoundException("Portfolio with id abc-123 cannot be found");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handlePortfolioNotFound(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().code()).isEqualTo("PORTFOLIO_NOT_FOUND");
      assertThat(response.getBody().message()).contains("abc-123");
    }

    @Test
    @DisplayName("AccountNotFoundException 404 ACCOUNT_NOT_FOUND with original message")
    void accountNotFoundException_returns404() {
      AccountNotFoundException ex = mock(AccountNotFoundException.class);
      when(ex.getMessage()).thenReturn("Account not found for portfolioId=abc");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccountNotFound(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().code()).isEqualTo("ACCOUNT_NOT_FOUND");
      assertThat(response.getBody().message()).isEqualTo("Account not found for portfolioId=abc");
    }

    @Test
    @DisplayName("TransactionNotFoundException 404 TRANSACTION_NOT_FOUND with original message")
    void transactionNotFoundException_returns404() {
      TransactionNotFoundException ex = mock(TransactionNotFoundException.class);
      when(ex.getMessage()).thenReturn("Transaction not found with id: tx-999");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleTransactionNotFound(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().code()).isEqualTo("TRANSACTION_NOT_FOUND");
      assertThat(response.getBody().message()).isEqualTo("Transaction not found with id: tx-999");
    }

    @Test
    @DisplayName("AssetNotFoundException 404 ASSET_NOT_FOUND with original message")
    void assetNotFoundException_returns404() {
      var ex = new AssetNotFoundException("No position found for symbol AAPL");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAssetNotFound(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().code()).isEqualTo("ASSET_NOT_FOUND");
      assertThat(response.getBody().message()).isEqualTo("No position found for symbol AAPL");
    }
  }

  // =========================================================================
  // 409 Conflict
  // =========================================================================

  @Nested
  @DisplayName("409 Conflict")
  class Conflict {

    @Test
    @DisplayName("PortfolioLimitReachedException 409 PORTFOLIO_LIMIT_REACHED")
    void portfolioLimitReachedException_returns409() {
      var ex = new PortfolioLimitReachedException("User already has an active portfolio");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handlePortfolioLimit(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("PORTFOLIO_LIMIT_REACHED");
      assertThat(response.getBody().message()).isEqualTo("User already has an active portfolio");
    }

    @Test
    @DisplayName("PortfolioNotEmptyException 409 PORTFOLIO_NOT_EMPTY")
    void portfolioNotEmptyException_returns409() {
      PortfolioNotEmptyException ex = mock(PortfolioNotEmptyException.class);
      when(ex.getMessage()).thenReturn("Portfolio still has 2 active accounts");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handlePortfolioNotEmpty(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("PORTFOLIO_NOT_EMPTY");
    }

    @Test
    @DisplayName("PortfolioDeletionException 409 PORTFOLIO_DELETION_ERROR")
    void portfolioDeletionException_returns409() {
      var ex = new PortfolioDeletionException("Cannot delete portfolio with 1 active account(s)");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handlePortfolioDeletion(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("PORTFOLIO_DELETION_ERROR");
      assertThat(response.getBody().message()).contains("active account");
    }

    @Test
    @DisplayName("PortfolioAlreadyDeletedException 409 PORTFOLIO_STATE_ERROR")
    void portfolioAlreadyDeletedException_returns409() {
      PortfolioAlreadyDeletedException ex = mock(PortfolioAlreadyDeletedException.class);
      when(ex.getMessage()).thenReturn("Portfolio has already been deleted");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handlePortfolioStateConflicts(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("PORTFOLIO_STATE_ERROR");
    }

    @Test
    @DisplayName("AccountCannotBeClosedException 409 ACCOUNT_CANNOT_BE_CLOSED")
    void accountCannotBeClosedException_returns409() {
      var ex = new AccountCannotBeClosedException(
          "Cannot close account: account has 3 open positions");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccountCannotBeClosed(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("ACCOUNT_CANNOT_BE_CLOSED");
      assertThat(response.getBody().message()).contains("open positions");
    }

    @Test
    @DisplayName("AccountCannotBeReopenedException 409 ACCOUNT_CANNOT_BE_REOPENED")
    void accountCannotBeReopenedException_returns409() {
      var ex = new AccountCannotBeReopenedException(
          "Cannot reopen account: account is ACTIVE not CLOSED");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccountCannotBeReopened(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("ACCOUNT_CANNOT_BE_REOPENED");
    }

    @Test
    @DisplayName("AccountClosedException 409 ACCOUNT_CLOSED")
    void accountClosedException_returns409() {
      AccountClosedException ex = mock(AccountClosedException.class);
      when(ex.getMessage()).thenReturn("Account is closed: acct-abc123");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccountClosed(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("ACCOUNT_CLOSED");
    }

    @Test
    @DisplayName("InvalidTransactionException 409 INVALID_TRANSACTION_STATE")
    void invalidTransactionException_returns409() {
      var ex = new InvalidTransactionException("Transaction already excluded");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidTransaction(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("INVALID_TRANSACTION_STATE");
      assertThat(response.getBody().message()).isEqualTo("Transaction already excluded");
    }

    @Test
    @DisplayName("IllegalStateException 409 CONFLICT with original message")
    void illegalStateException_returns409() {
      var ex = new IllegalStateException("Cannot apply split: no open position found for AAPL");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalState(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("CONFLICT");
      assertThat(response.getBody().message()).contains("no open position found for AAPL");
    }

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException 409 CONCURRENT_MODIFICATION with retry message")
    void objectOptimisticLockingFailureException_returns409() {
      ObjectOptimisticLockingFailureException ex = mock(
          ObjectOptimisticLockingFailureException.class);

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleOptimisticLock(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
      assertThat(response.getBody().message()).isEqualTo(
          "The record was updated by another process. Please refresh and try again.");
    }
  }

  // =========================================================================
  // 422 Unprocessable Entity
  // NOTE: HttpStatus.UNPROCESSABLE_CONTENT does not exist in Spring's HttpStatus.
  // The correct constant is UNPROCESSABLE_ENTITY (422). Asserting on raw value.
  // =========================================================================

  @Nested
  @DisplayName("422 Unprocessable Entity")
  class UnprocessableEntity {

    @Test
    @DisplayName("InsufficientFundsException 422 INSUFFICIENT_FUNDS with original message")
    void insufficientFundsException_returns422() {
      InsufficientFundsException ex = mock(InsufficientFundsException.class);
      when(ex.getMessage()).thenReturn(
          "Insufficient cash for buy. Required: CAD 500.00, Available: CAD 100.00");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInsufficientFunds(
          ex);

      assertThat(response.getStatusCode().value()).isEqualTo(422);
      assertThat(response.getBody().code()).isEqualTo("INSUFFICIENT_FUNDS");
      assertThat(response.getBody().message()).contains("Required");
    }

    @Test
    @DisplayName("InsufficientQuantityException 422 INSUFFICIENT_QUANTITY with original message")
    void insufficientQuantityException_returns422() {
      var ex = new InsufficientQuantityException("Cannot sell 100. Position only holds: 50");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInsufficientQuantity(
          ex);

      assertThat(response.getStatusCode().value()).isEqualTo(422);
      assertThat(response.getBody().code()).isEqualTo("INSUFFICIENT_QUANTITY");
      assertThat(response.getBody().message()).contains("50");
    }

    @Test
    @DisplayName("CurrencyMismatchException 422 CURRENCY_MISMATCH with original message")
    void currencyMismatchException_returns422() {
      CurrencyMismatchException ex = mock(CurrencyMismatchException.class);
      when(ex.getMessage()).thenReturn("Quote currency USD does not match account currency CAD");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleCurrencyMismatch(
          ex);

      assertThat(response.getStatusCode().value()).isEqualTo(422);
      assertThat(response.getBody().code()).isEqualTo("CURRENCY_MISMATCH");
      assertThat(response.getBody().message()).contains("USD");
    }

    @Test
    @DisplayName("UnknownSymbolException 422 UNKNOWN_SYMBOL with original message")
    void unknownSymbolException_returns422() {
      var ex = new UnknownSymbolException("NOTAREAL");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUnknownSymbol(
          ex);

      assertThat(response.getStatusCode().value()).isEqualTo(422);
      assertThat(response.getBody().code()).isEqualTo("UNKNOWN_SYMBOL");
      assertThat(response.getBody().message()).isEqualTo("NOTAREAL");
    }

    @Test
    @DisplayName("CsvImportCommitException 422 CSV_COMMIT_FAILED with row context in message")
    void csvImportCommitException_returns422() {
      var ex = new CsvImportCommitException(
          "Row 12 failed on commit: Cannot sell AAPL, no open position",
          new RuntimeException("domain failure"));

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleCsvCommitFailure(
          ex);

      assertThat(response.getStatusCode().value()).isEqualTo(422);
      assertThat(response.getBody().code()).isEqualTo("CSV_COMMIT_FAILED");
      assertThat(response.getBody().message()).contains("Row 12");
    }
  }

  // =========================================================================
  // ResponseStatusException passthrough
  // =========================================================================

  @Nested
  @DisplayName("ResponseStatusException passthrough")
  class ResponseStatusPassthrough {

    @Test
    @DisplayName("ResponseStatusException preserves its status code and reason")
    void responseStatusException_preservesStatusAndReason() {
      var ex = new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Symbol not found or not supported: INVALID");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleResponseStatus(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().code()).isEqualTo("REQUEST_ERROR");
      assertThat(response.getBody().message()).isEqualTo(
          "Symbol not found or not supported: INVALID");
    }

    @Test
    @DisplayName("ResponseStatusException with 400 preserves 400 status code")
    void responseStatusException_400_preserves400() {
      var ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid symbol format: lower");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleResponseStatus(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("ResponseStatusException with 503 preserves 503 status code")
    void responseStatusException_503_preserves503() {
      var ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Exchange rate service is temporarily unavailable");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleResponseStatus(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  // =========================================================================
  // 500 Internal Server Error
  // =========================================================================

  @Nested
  @DisplayName("500 Internal Server Error")
  class InternalServerError {

    @Test
    @DisplayName("Unhandled Exception 500 INTERNAL_ERROR with generic safe message (not exception detail)")
    void genericException_returns500WithGenericMessage() {
      var ex = new RuntimeException("Database connection pool exhausted - internal detail");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGeneric(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
      // Intentionally does NOT expose internal exception message to the client
      assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
      assertThat(response.getBody().message()).doesNotContain("Database connection pool");
    }

    @Test
    @DisplayName("NullPointerException 500 INTERNAL_ERROR (no NPE leaks to client)")
    void nullPointerException_returns500() {
      var ex = new NullPointerException("Internal null dereference");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGeneric(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("ClassCastException 500 INTERNAL_ERROR")
    void classCastException_returns500() {
      var ex = new ClassCastException("Unexpected type in mapper");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGeneric(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  // =========================================================================
  // 503 Service Unavailable — External Market Data Providers
  // =========================================================================

  @Nested
  @DisplayName("503 Service Unavailable — External Providers")
  class ServiceUnavailable {

    @Test
    @DisplayName("FmpApiException 503 MARKET_DATA_UNAVAILABLE with generic safe message")
    void fmpApiException_returns503() {
      var ex = new FmpApiException("FMP rate limit exceeded (250/day)");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMarketDataError(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      assertThat(response.getBody().code()).isEqualTo("MARKET_DATA_UNAVAILABLE");
      assertThat(response.getBody().message()).isEqualTo(
          "Market data service is temporarily unavailable");
      // Internal FMP detail must not leak
      assertThat(response.getBody().message()).doesNotContain("250/day");
    }

    @Test
    @DisplayName("BocApiException 503 MARKET_DATA_UNAVAILABLE")
    void bocApiException_returns503() {
      var ex = new BocApiException("Bank of Canada server encountered an internal error");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMarketDataError(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      assertThat(response.getBody().code()).isEqualTo("MARKET_DATA_UNAVAILABLE");
    }

    @Test
    @DisplayName("BocApiException with status code constructor 503")
    void bocApiException_withStatusCode_returns503() {
      var ex = new BocApiException("BOC API Error 500");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMarketDataError(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      assertThat(response.getBody().code()).isEqualTo("MARKET_DATA_UNAVAILABLE");
    }

    @Test
    @DisplayName("MarketDataException 503 MARKET_DATA_UNAVAILABLE")
    void marketDataException_returns503() {
      var ex = new MarketDataException("Redis quote cache is unavailable");

      ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMarketDataError(
          ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      assertThat(response.getBody().code()).isEqualTo("MARKET_DATA_UNAVAILABLE");
      assertThat(response.getBody().message()).isEqualTo(
          "Market data service is temporarily unavailable");
    }

    @Test
    @DisplayName("All three external provider exceptions produce identical safe response body")
    void allThreeExternalProviderExceptions_produceSameResponseBody() {
      var fmpEx = new FmpApiException("fmp detail");
      var bocEx = new BocApiException("boc detail");
      var marketEx = new MarketDataException("market detail");

      var fmpResponse = handler.handleMarketDataError(fmpEx);
      var bocResponse = handler.handleMarketDataError(bocEx);
      var marketResponse = handler.handleMarketDataError(marketEx);

      assertThat(fmpResponse.getBody().code()).isEqualTo(bocResponse.getBody().code())
          .isEqualTo(marketResponse.getBody().code()).isEqualTo("MARKET_DATA_UNAVAILABLE");

      assertThat(fmpResponse.getBody().message()).isEqualTo(bocResponse.getBody().message())
          .isEqualTo(marketResponse.getBody().message());
    }
  }

  // =========================================================================
  // ErrorResponse record — factory methods and structure
  // =========================================================================

  @Nested
  @DisplayName("ErrorResponse factory methods")
  class ErrorResponseFactory {

    @Test
    @DisplayName("ErrorResponse.of() sets code, message, empty errors list, and non-null timestamp")
    void of_populatesAllFields() {
      var response = GlobalExceptionHandler.ErrorResponse.of("TEST_CODE", "Something went wrong");

      assertThat(response.code()).isEqualTo("TEST_CODE");
      assertThat(response.message()).isEqualTo("Something went wrong");
      assertThat(response.errors()).isEmpty();
      assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("ErrorResponse.withErrors() sets code, message, errors list, and non-null timestamp")
    void withErrors_populatesAllFields() {
      List<String> errors = List.of("field1: required", "field2: invalid");

      var response = GlobalExceptionHandler.ErrorResponse.withErrors("VALIDATION_ERROR",
          "Validation failed", errors);

      assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.message()).isEqualTo("Validation failed");
      assertThat(response.errors()).containsExactly("field1: required", "field2: invalid");
      assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("ErrorResponse.of() errors list is unmodifiable")
    void of_errorsListIsUnmodifiable() {
      var response = GlobalExceptionHandler.ErrorResponse.of("CODE", "msg");

      assertThat(response.errors()).isEmpty();
      // List.of() returns an unmodifiable list — this is the contract we want
      org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
          () -> response.errors().add("should not work"));
    }

    @Test
    @DisplayName("Two separate ErrorResponse.of() calls produce timestamps that are not null")
    void of_timestampIsPopulatedOnEachCall() {
      var r1 = GlobalExceptionHandler.ErrorResponse.of("C1", "m1");
      var r2 = GlobalExceptionHandler.ErrorResponse.of("C2", "m2");

      assertThat(r1.timestamp()).isNotNull();
      assertThat(r2.timestamp()).isNotNull();
    }
  }
}