package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.ErrorType;

class MarketDataExceptionTest {

private static final String SYMBOL = "AAPL";
    private static final String MESSAGE = "Custom Error Message";

    @Test
    void testConstructors() {
        // Test basic constructor
        MarketDataException ex1 = new MarketDataException(MESSAGE, ErrorType.PROVIDER_ERROR);
        assertEquals(MESSAGE, ex1.getMessage());
        assertEquals(ErrorType.PROVIDER_ERROR, ex1.getErrorType());
        assertNull(ex1.getSymbol());

        // Test constructor with symbol
        MarketDataException ex2 = new MarketDataException(MESSAGE, SYMBOL, ErrorType.DATA_UNAVAILABLE);
        assertEquals(SYMBOL, ex2.getSymbol());

        // Test constructor with cause
        Throwable cause = new RuntimeException("Original error");
        MarketDataException ex3 = new MarketDataException(MESSAGE, cause, ErrorType.NETWORK_ERROR);
        assertEquals(cause, ex3.getCause());

        // Test constructor with symbol and cause
        MarketDataException ex4 = new MarketDataException(MESSAGE, SYMBOL, cause, ErrorType.SYMBOL_NOT_FOUND);
        assertEquals(SYMBOL, ex4.getSymbol());
        assertEquals(cause, ex4.getCause());
    }

    @Test
    void symbolNotFound_Factory_ShouldSetCorrectFields() {
        MarketDataException ex = MarketDataException.symbolNotFound(SYMBOL);
        
        assertEquals(String.format("Symbol '%s' not found", SYMBOL), ex.getMessage());
        assertEquals(SYMBOL, ex.getSymbol());
        assertEquals(ErrorType.SYMBOL_NOT_FOUND, ex.getErrorType());
    }

    @Test
    void symbolNotSupported_Factory_ShouldSetCorrectFields() {
        MarketDataException ex = MarketDataException.symbolNotSupported(SYMBOL);
        
        assertEquals(String.format("Symbol '%s' is not supported by this provider", SYMBOL), ex.getMessage());
        assertEquals(SYMBOL, ex.getSymbol());
        assertEquals(ErrorType.SYMBOL_NOT_SUPPORTED, ex.getErrorType());
    }

    @Test
    void dataUnavailable_Factory_ShouldIncludeReason() {
        String reason = "Market closed";
        MarketDataException ex = MarketDataException.dataUnavailable(SYMBOL, reason);
        
        assertTrue(ex.getMessage().contains(SYMBOL));
        assertTrue(ex.getMessage().contains(reason));
        assertEquals(ErrorType.DATA_UNAVAILABLE, ex.getErrorType());
    }

    @Test
    void rateLimitExceeded_Factory_ShouldHaveStaticMessage() {
        MarketDataException ex = MarketDataException.rateLimitExceeded();
        
        assertEquals("API rate limit exceeded. Please try again later.", ex.getMessage());
        assertEquals(ErrorType.RATE_LIMIT_EXCEEDED, ex.getErrorType());
        assertNull(ex.getSymbol());
    }

    @Test
    void providerError_Factory_ShouldWrapCause() {
        Throwable cause = new IllegalStateException("API Key Invalid");
        MarketDataException ex = MarketDataException.providerError("Auth Failure", cause);
        
        assertEquals("Market data provider error: Auth Failure", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(ErrorType.PROVIDER_ERROR, ex.getErrorType());
    }

    @Test
    void networkError_Factory_ShouldWrapCause() {
        Throwable cause = new java.net.ConnectException("Timeout");
        MarketDataException ex = MarketDataException.networkError(cause);
        
        assertEquals("Network error while fetching market data", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(ErrorType.NETWORK_ERROR, ex.getErrorType());
    }

}