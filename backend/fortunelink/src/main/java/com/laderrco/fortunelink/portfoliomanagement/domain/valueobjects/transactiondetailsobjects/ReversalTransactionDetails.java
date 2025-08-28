package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;

public class ReversalTransactionDetails extends TransactionDetails {
    private final TransactionId transactionId;
    private final String reason;
    // NOTE: reversal date is dirved from the Transaction.java class
    protected ReversalTransactionDetails(TransactionId transactionId, String reason, TransactionSource source, String description, List<Fee> fees) {
        super(source, description, fees);
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction id cannot be null.");
        reason = Objects.requireNonNull(reason, "Reason cannot be null.");

        reason = reason.trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be blank");
        }

        if (reason.length() > 1000) {
            throw new IllegalArgumentException("Reason for reversal cannot be greater than 1000 characters.");
        }

        this.reason = reason;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public String getReason() {
        return reason;
    }

    
}
