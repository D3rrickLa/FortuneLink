package com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions;

public class BocApiException extends RuntimeException {
    public BocApiException(String message) {
        super(message);
    }

    public BocApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
