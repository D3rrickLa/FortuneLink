package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record PortfolioResponse(PortfolioId portfolioId, UserId userId, List<Account> accounts, Money totalValue, int transactionCount, Instant createDate, Instant lastUpdated) {
}