
package com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.AccountNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioAlreadyExistsException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioDeletionRequiresConfirmationException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.SymbolNotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST API.
 * Translates domain/infrastructure exceptions into proper HTTP responses.
 * 
 * Benefits:
 * - Centralized error handling
 * - Consistent error response format
 * - Proper HTTP status codes
 * - Security (no stack traces leaked to clients)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- 404 NOT FOUND ---
    @ExceptionHandler({
            PortfolioNotFoundException.class,
            AccountNotFoundException.class,
            AssetNotFoundException.class,
            SymbolNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    // --- 400 BAD REQUEST / VALIDATION ---
    @ExceptionHandler({
            IllegalArgumentException.class,
            InvalidCommandException.class,
            InvalidTransactionException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    // @ExceptionHandler(MethodArgumentNotValidException.class)
    // public ResponseEntity<List<String>> handleValidationErrors(MethodArgumentNotValidException ex) {
    //     List<String> errors = ex.getBindingResult()
    //             .getFieldErrors()
    //             .stream()
    //             .map(error -> error.getField() + ": " + error.getDefaultMessage())
    //             .collect(Collectors.toList());
    //     return ResponseEntity.badRequest().body(errors);
    // }

    // --- 409 CONFLICT (Business Rules) ---
    @ExceptionHandler({
            PortfolioAlreadyExistsException.class,
            AccountNotEmptyException.class,
            PortfolioNotEmptyException.class,
            PortfolioDeletionRequiresConfirmationException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessConflicts(RuntimeException ex, WebRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        return buildResponse(
                (HttpStatus) ex.getStatusCode(),
                ex.getStatusCode().toString(),
                ex.getReason(),
                null,
                request);
    }

    // --- 503 SERVICE UNAVAILABLE ---
    @ExceptionHandler(MarketDataException.class)
    public ResponseEntity<ErrorResponse> handleMarketDataException(MarketDataException ex, WebRequest request) {
        log.error("External service error: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "Market data service is temporarily unavailable.", ex.getMessage(), request);
    }

    // --- 500 INTERNAL SERVER ERROR ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.",
                request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, String details,
            WebRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .details(details)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message,
            WebRequest request) {
        return buildResponse(status, error, message, " ", request);
    }

    /**
     * Standard error response structure.
     * Consistent format across all error responses.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String details; // Optional: additional context
        private String path;
    }
}