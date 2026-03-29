package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.IdentifiedTransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record RestoreTransactionCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, 
  TransactionId transactionId) implements IdentifiedTransactionCommand {
}