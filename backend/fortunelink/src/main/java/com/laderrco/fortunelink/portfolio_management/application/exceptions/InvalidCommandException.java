package com.laderrco.fortunelink.portfolio_management.application.exceptions;

import java.util.List;

public class InvalidCommandException extends RuntimeException {

    public InvalidCommandException(String string, List<String> errors) {
        String s = String.format("Error, portfolio: %s. Issue involving %s", string, errors);
        super(s);
    }
    
}
