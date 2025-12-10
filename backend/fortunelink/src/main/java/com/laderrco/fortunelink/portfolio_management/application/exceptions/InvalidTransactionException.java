package com.laderrco.fortunelink.portfolio_management.application.exceptions;

import java.util.List;

public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String s) {
        super(s); // need message and validationerrors to be passed
    }

    public InvalidTransactionException(String s, List<String> errors) {
        super(String.format("Error with message: %s. List of validation errors", s, errors.stream()));
    }
}
