package com.laderrco.fortunelink.shared.exceptions;

public final class InsufficientFundsException extends RuntimeException{
    public InsufficientFundsException(String message) {
        super(message);
    }
}
