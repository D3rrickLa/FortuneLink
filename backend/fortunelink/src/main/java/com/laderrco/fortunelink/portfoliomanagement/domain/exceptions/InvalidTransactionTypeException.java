package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class InvalidTransactionTypeException extends RuntimeException {
    public InvalidTransactionTypeException(String message) {
        super(message);
    }
}