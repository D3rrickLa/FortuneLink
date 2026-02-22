package com.laderrco.fortunelink.portfolio.application.exceptions;

import java.util.List;

public class InvalidCommandException extends RuntimeException {

    public InvalidCommandException(String message, List<String> errors) {
        String s = String.format("%s: %s", message, errors);
        super(s);
    }

}
