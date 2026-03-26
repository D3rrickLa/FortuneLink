package com.laderrco.fortunelink.portfolio.application.utils.valueobjects;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;

public record PortfolioContext(Portfolio portfolio, Account account) {
}