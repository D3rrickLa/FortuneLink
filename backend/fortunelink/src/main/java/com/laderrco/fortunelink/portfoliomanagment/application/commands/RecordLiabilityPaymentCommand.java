package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.time.Instant;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;

public record RecordLiabilityPaymentCommand(
    UUID portfolioId,
    UUID liabilityId,
    Money paymentAmount,
    Instant paymentDate,
    TransactionMetadata metadata
) {
    
}
