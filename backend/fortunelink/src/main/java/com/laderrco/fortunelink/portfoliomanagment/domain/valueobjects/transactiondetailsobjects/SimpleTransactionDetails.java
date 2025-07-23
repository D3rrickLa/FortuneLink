package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.Objects;

public final class SimpleTransactionDetails extends TransactionDetails {
    private final String description;

    public SimpleTransactionDetails(String description) {
        Objects.requireNonNull(description, "Description cannot be null.");
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
