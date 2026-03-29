package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.IdentifiedTransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;


/**
 * Marks a transaction as excluded from position and capital gains calculation.
 * <p>
 * CASH BALANCE NOTE: Excluding a trade does NOT reverse the cash impact. The cash balance reflects
 * actual money movement. Only ACB/position state is recalculated. This is INTENTIONAL - your
 * brokerage statement, not this app, is the source of truth for cash.
 */
public record ExcludeTransactionCommand(PortfolioId portfolioId, UserId userId, AccountId accountId,
    TransactionId transactionId, String reason) implements IdentifiedTransactionCommand {
}