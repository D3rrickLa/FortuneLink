package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.time.Instant;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;

public record RecordCashDepositCommand(
    UUID portfolioId,
    Money amount,
    Instant transactionDate,
    TransactionMetadata transactionMetadata
) {
    
}
