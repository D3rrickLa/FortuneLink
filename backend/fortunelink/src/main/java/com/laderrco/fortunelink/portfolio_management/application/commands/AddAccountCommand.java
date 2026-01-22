package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public record AddAccountCommand(PortfolioId portfolioId, String accountName, AccountType accountType, ValidatedCurrency baseCurrency) {
}