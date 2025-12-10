package com.laderrco.fortunelink.portfolio_management.application.exceptions;

public class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(String s) { // this should be a userId
        super(s);
    }
}
