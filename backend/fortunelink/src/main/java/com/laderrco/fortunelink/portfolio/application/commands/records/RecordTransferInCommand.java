package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record RecordTransferInCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, Money amount,
    List<Fee> fees, String notes, Instant transactionDate) implements TransactionCommand {
}
