package com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions;

public class SymbolNotFoundException extends RuntimeException{
    public SymbolNotFoundException(String s) {
        super(s);
    }
}
