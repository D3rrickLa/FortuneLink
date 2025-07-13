package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;

public record CommonTransactionInput(
    // NOTE: an Anemic DTO issue if we don't watch out
    // KISS. If this ever becomes complex, we might need to shift some of the logic to a factory or builder
    UUID correlationId,
    UUID parentTransactionId,
    TransactionType transactionType,
    TransactionMetadata transactionMetadata,
    List<Fee> fees
) {
    public CommonTransactionInput {
        Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");
        Objects.requireNonNull(correlationId, "Correlation id cannot be null.");

        fees = fees != null ? fees : Collections.emptyList();
    }
}
