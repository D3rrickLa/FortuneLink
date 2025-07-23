package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.time.Instant;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;

public record ReverseTransactionCommand(
    UUID portfolioId,
    UUID transactionId,
    Instant reversalDate,
    String reason,
    TransactionMetadata transactionMetadata
) {
    
}
