package com.laderrco.fortunelink.portfolio_management.application.exceptions;

public class PortfolioLimitReachedException extends RuntimeException {
    public PortfolioLimitReachedException(String s) {
        super(s);
    }
}
