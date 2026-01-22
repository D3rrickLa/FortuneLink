package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

public class PortfolioAlreadyDeletedException extends RuntimeException{

    public PortfolioAlreadyDeletedException(String string) {
        super(string);
    }

}
