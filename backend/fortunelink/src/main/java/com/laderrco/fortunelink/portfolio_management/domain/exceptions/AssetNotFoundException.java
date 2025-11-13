package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(String message) {
        super(message);
    }
}
