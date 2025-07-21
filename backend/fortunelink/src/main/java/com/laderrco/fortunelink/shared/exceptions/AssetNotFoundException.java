package com.laderrco.fortunelink.shared.exceptions;

public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(String message) {
        super(message);
    }
}
