package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class InvalidReversalTransactionException extends RuntimeException {
    public InvalidReversalTransactionException(String message) {
        super(message);
    }

}