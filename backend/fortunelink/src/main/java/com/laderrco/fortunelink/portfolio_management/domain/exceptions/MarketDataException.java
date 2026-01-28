package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.ErrorType;

/**
 * Domain exception for market data retrieval failures.
 * 
 * This exception represents business-level failures in obtaining market data,
 * not technical HTTP/network errors (those are wrapped).
 */
public class MarketDataException extends RuntimeException {
    
    private final ErrorType errorType;
    private final String symbol;
    
    public MarketDataException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.symbol = null;
    }
    
    public MarketDataException(String message, String symbol, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.symbol = symbol;
    }
    
    public MarketDataException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
        this.symbol = null;
    }
    
    public MarketDataException(String message, String symbol, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
        this.symbol = symbol;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public String getSymbol() {
        return symbol;
    }

    // Static factory methods for common scenarios //
    
    public static MarketDataException symbolNotFound(String symbol) {
        return new MarketDataException(
            String.format("Symbol '%s' not found", symbol),
            symbol,
            ErrorType.SYMBOL_NOT_FOUND
        );
    }
    
    public static MarketDataException symbolNotSupported(String symbol) {
        return new MarketDataException(
            String.format("Symbol '%s' is not supported by this provider", symbol),
            symbol,
            ErrorType.SYMBOL_NOT_SUPPORTED
        );
    }
    
    public static MarketDataException dataUnavailable(String symbol, String reason) {
        return new MarketDataException(
            String.format("Data unavailable for '%s': %s", symbol, reason),
            symbol,
            ErrorType.DATA_UNAVAILABLE
        );
    }
    
    public static MarketDataException rateLimitExceeded() {
        return new MarketDataException(
            "API rate limit exceeded. Please try again later.",
            ErrorType.RATE_LIMIT_EXCEEDED
        );
    }
    
    public static MarketDataException providerError(String message, Throwable cause) {
        return new MarketDataException(
            "Market data provider error: " + message,
            cause,
            ErrorType.PROVIDER_ERROR
        );
    }
    
    public static MarketDataException networkError(Throwable cause) {
        return new MarketDataException(
            "Network error while fetching market data",
            cause,
            ErrorType.NETWORK_ERROR
        );
    }
}