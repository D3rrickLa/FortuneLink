package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;

public record RecordNewLiabilityCommand(
    UUID portfolioId,
    Money originalLoanAmount, // Original amount of the loan/liability (in its native currency)
    Percentage annualInterestRate,
    Instant incurrenceDate,
    Instant maturityDate,
    String description,
    List<Fee> fees,
    TransactionMetadata transactionMetadata
) {
    
}
