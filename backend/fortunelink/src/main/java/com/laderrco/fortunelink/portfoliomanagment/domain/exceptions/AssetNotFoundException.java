package com.laderrco.fortunelink.portfoliomanagment.domain.exceptions;

public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(String msg) {
        super(msg);
    }
}
