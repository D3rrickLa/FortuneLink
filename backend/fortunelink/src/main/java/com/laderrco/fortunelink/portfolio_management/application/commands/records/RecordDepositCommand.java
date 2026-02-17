package com.laderrco.fortunelink.portfolio_management.application.commands.records;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record RecordDepositCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, Money amount, List<Fee> fees, Instant transactionDate, String notes) {
}