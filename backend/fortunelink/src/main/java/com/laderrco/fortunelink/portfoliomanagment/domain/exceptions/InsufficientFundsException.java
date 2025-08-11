package com.laderrco.fortunelink.portfoliomanagment.domain.exceptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String msg) {
        super(msg);
    }
}
