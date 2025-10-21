package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class AssetNotFoundException extends RuntimeException{
    public AssetNotFoundException(String s) {
        super(s);
    }
}
