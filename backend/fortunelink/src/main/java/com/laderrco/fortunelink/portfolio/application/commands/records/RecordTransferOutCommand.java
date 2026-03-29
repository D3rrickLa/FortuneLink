package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.TransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record RecordTransferOutCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, Money amount,
    Instant transactionDate, String notes) implements TransactionCommand {

}
