package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record RecordSplitCommand(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    String symbol,
    Ratio ratio, // e.g. Ratio(2, 1) for a 2-for-1 split
    Instant transactionDate,
    String notes) implements TransactionCommand {
}