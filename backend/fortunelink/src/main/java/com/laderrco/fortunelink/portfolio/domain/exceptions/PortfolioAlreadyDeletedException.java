package com.laderrco.fortunelink.portfolio.domain.exceptions;

public class PortfolioAlreadyDeletedException extends RuntimeException {

    public PortfolioAlreadyDeletedException(String message) {
        super(message);
    }

}
