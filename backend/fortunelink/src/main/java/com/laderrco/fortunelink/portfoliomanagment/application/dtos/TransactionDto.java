package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TransactionDto(
    UUID transactionId,
    Instant transactionDate,
    String transactionType,
    String description,
    String currencyCode,
    BigDecimal amount,         // The primary amount (e.g., original deposit, purchase cost, loan payment)
    BigDecimal cashFlowImpact, // The net change in portfolio cash (positive for inflow, negative for outflow)
    String relatedEntityName,  // e.g., "AAPL", "Mortgage Loan"
    UUID relatedEntityId       // The ID of the related asset, liability, etc.
) {
    public TransactionDto {
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null.");
        Objects.requireNonNull(description, "Description cannot be null.");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(cashFlowImpact, "Cash flow impact cannot be null.");
        // relatedEntityName and relatedEntityId can be null, so no requireNonNull for them
    }
}
