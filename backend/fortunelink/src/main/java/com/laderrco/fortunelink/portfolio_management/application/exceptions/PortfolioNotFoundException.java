package com.laderrco.fortunelink.portfolio_management.application.exceptions;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

public class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(String s) { // this should be a userId
        super(s);
    }

    public PortfolioNotFoundException(UserId userId) { // this should be a userId
        super("Portfolio can't be found with user id: " + userId.toString());
    }
}
