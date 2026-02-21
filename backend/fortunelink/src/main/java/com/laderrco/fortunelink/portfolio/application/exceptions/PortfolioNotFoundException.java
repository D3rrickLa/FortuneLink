package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String message) {
        super(message);
    }

    public PortfolioNotFoundException(PortfolioId portfolioId) {
        super(String.format("Portfolio with id %s cannot be found", portfolioId.id().toString()));
    }
    
}
