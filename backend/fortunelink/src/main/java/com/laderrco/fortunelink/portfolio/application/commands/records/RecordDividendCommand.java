package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

// Non-DRIP dividend - cash hits the account
public record RecordDividendCommand(PortfolioId portfolioId, UserId userId, AccountId accountId,
        String assetSymbol, Money amount, Instant transactionDate, String notes) implements TransactionCommand {
}