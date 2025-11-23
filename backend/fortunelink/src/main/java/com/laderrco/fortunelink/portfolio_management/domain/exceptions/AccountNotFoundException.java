package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import java.util.NoSuchElementException;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String e) {
        super(e);
    }

    public AccountNotFoundException(NoSuchElementException e) {
        super(e);
    }
}
