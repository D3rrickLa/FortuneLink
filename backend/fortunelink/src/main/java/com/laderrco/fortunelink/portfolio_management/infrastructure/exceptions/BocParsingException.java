package com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;

public class BocParsingException extends RuntimeException {
    public BocParsingException(String string, JsonProcessingException e) {
        super(string, e);
    }
}
