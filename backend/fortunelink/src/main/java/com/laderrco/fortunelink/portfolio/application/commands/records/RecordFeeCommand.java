package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.AdditionalInfoTransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.UUID;

// just the total
public record RecordFeeCommand(
    UUID idempotencyKey,
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    Money amount,
    FeeType feeType,
    Instant transactionDate,
    String notes) implements AdditionalInfoTransactionCommand {
}