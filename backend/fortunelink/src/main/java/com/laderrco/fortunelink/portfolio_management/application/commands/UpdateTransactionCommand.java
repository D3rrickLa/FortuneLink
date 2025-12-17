package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record UpdateTransactionCommand(
    UserId userId,
    TransactionId transactionId,
    AccountId accountId,
    TransactionType type,
    String symbol,
    BigDecimal quantity,
    Money price,
    Fee fee,
    Instant date,
    String notes
) {}