package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

public class InvalidAssetSymbolException extends RuntimeException {
    public InvalidAssetSymbolException(String message) {
        super(message);
    }
}
