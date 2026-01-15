package com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;

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

    /**
     * Handle symbol not found (404 Not Found).
     */
    @ExceptionHandler(SymbolNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSymbolNotFound(
            SymbolNotFoundException ex, 
            WebRequest request) {
        
        log.warn("Symbol not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(getPath(request))
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle general market data errors (503 Service Unavailable).
     * This includes API rate limits, network issues, etc.
     */
    @ExceptionHandler(MarketDataException.class)
    public ResponseEntity<ErrorResponse> handleMarketDataException(
            MarketDataException ex, 
            WebRequest request) {
        
        log.error("Market data service error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Market data service is temporarily unavailable. Please try again later.")
                .details(ex.getMessage())
                .path(getPath(request))
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle illegal arguments (400 Bad Request).
     * E.g., invalid symbol format
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, 
            WebRequest request) {
        
        log.warn("Invalid request: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(getPath(request))
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Catch-all for unexpected errors (500 Internal Server Error).
     * IMPORTANT: Don't leak internal details to clients!
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(getPath(request))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Extract request path from WebRequest.
     */
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
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
        private String details;  // Optional: additional context
        private String path;
    }
}