package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

public class IllegalStatusTransitionException extends RuntimeException{
    public IllegalStatusTransitionException(String s) {
        super(s);
    }
}
