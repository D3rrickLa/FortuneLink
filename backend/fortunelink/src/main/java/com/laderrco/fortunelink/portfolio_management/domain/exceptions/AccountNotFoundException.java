package com.laderrco.fortunelink.portfolio_management.domain.exceptions;


public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String e) {
        super(e);
    }
}
