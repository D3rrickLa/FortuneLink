package com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions;

/**
 * Exception thrown when FMP API calls fail.
 * 
 * Common scenarios:
 * - Symbol not found (empty response)
 * - Invalid API key (401)
 * - Rate limit exceeded (429)
 * - Network errors
 * - JSON parsing errors
 */
public class FmpApiException extends RuntimeException {

    public FmpApiException(String message) {
        super(message);
    }

    public FmpApiException(String message, Throwable cause) {
        super(message, cause);
    }
}