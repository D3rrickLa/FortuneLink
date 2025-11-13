package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

public class UnsupportedTransactionTypeException extends RuntimeException {
    public UnsupportedTransactionTypeException(String message) {
        super(message);
    }
}
