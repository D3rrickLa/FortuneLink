package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String e) {
        super(e);
    }

    public AccountNotFoundException(AccountId accountId, PortfolioId portfolioId) {
        super(String.format("cannot find %s in portfolio %s", accountId, portfolioId));
    }
}
