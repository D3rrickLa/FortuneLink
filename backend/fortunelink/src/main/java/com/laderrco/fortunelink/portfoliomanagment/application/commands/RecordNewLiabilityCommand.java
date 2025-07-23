package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.time.Instant;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;

public record RecordNewLiabilityCommand(
    UUID portfolioId,
    Money originalLoanAmount,
    Percentage annualInterestRate,
    Instant incurrenceDate,
    Instant maturityDate,
    String description, // e.g., "Car Loan", "Mortgage"
    TransactionMetadata metadata
) {
    
}
