package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String s) {
        super(s);
    }
}
