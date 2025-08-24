package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class TransactionAlreadyReversedException extends RuntimeException {
    public TransactionAlreadyReversedException(String s) {
        super(s);
    }
    
}