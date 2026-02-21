package com.laderrco.fortunelink.portfolio.application.commands;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.application.commands.records.TransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;


public record UpdateTransactionCommand(
        PortfolioId portfolioId,
        UserId userId,
        AccountId accountId,
        TransactionId transactionId,
        TransactionType type,
        String symbol,
        Quantity quantity,
        Price price,
        List<Fee> fees,
        Instant date,
        String notes) implements TransactionCommand {
}