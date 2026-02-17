package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;


public record UpdateTransactionCommand(
        PortfolioId portfolioId,
        UserId userId,
        AccountId accountId,
        TransactionId transactionId,
        TransactionType type,
        AssetSymbol symbol,
        Quantity quantity,
        Price price,
        List<Fee> fee,
        Instant date,
        String notes) {
}