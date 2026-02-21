package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;

public interface PortfolioValuationService {
    Money calculateTotalValue(Portfolio portfolio, Currency targetCurrency);
    Money calculateAccountValue(Account account);
    Money calculatePositionsValue(Account account);
}