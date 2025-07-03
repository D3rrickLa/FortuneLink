package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Objects;
import java.util.UUID;

// voidingTransactionId = the new Id that would void the transaction assigned to this 'transaction previous'
public record VoidInfo(UUID voidingTransactionId) {
    public VoidInfo {
        Objects.requireNonNull(voidingTransactionId);
    }
}
