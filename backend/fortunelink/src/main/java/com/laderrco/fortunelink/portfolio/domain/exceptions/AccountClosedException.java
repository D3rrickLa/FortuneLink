package com.laderrco.fortunelink.portfolio.domain.exceptions;

public class AccountClosedException extends RuntimeException {
    public AccountClosedException(String s) {
        super(s);
    }
}
