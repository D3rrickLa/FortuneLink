package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class InvalidTransactionAmountException extends RuntimeException {
    public InvalidTransactionAmountException(String message) {
        super(message);
    }
}