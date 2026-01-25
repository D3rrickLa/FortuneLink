package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

public class SymbolNotFoundException extends RuntimeException{
    public SymbolNotFoundException(String s) {
        super(s);
    }
}
