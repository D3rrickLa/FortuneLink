package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class InvalidTransactionDateException extends RuntimeException {
    public InvalidTransactionDateException(String message) {
        super(message);
    }
}