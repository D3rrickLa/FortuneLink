package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class InsufficientHoldingException extends RuntimeException{
    public InsufficientHoldingException(String s){
        super(s);
    }
}
