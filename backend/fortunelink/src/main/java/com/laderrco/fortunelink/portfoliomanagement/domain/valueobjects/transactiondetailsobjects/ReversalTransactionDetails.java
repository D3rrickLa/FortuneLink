package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.ReversalReason;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.ReversalType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;

public final class ReversalTransactionDetails extends TransactionDetails {
    private final TransactionId originalTransactionId;
    private final ReversalReason reason;
    private final ReversalType reversalType; // should be named something else...
    private final Currency currency; // used for the ZERO
    private final String initiatedBy; // user/system
    private final Instant reversedAt;
    private final String note;
    
    public ReversalTransactionDetails(TransactionId originalTransactionId, ReversalReason reason, ReversalType reversalType, Currency currency, String initiatedBy, Instant reversedAt, String note, TransactionSource source, String description, List<Fee> fees) {
        super(source, description, fees);
        this.originalTransactionId = Objects.requireNonNull(originalTransactionId);
        this.reason = Objects.requireNonNull(reason);
        this.reversalType = Objects.requireNonNull(reversalType);
        this.currency = currency;
        this.initiatedBy = initiatedBy;
        this.reversedAt = Objects.requireNonNull(reversedAt);
        this.note = note.trim();
    }
    
    
    @Override
    public Money calculateNetImpact(TransactionType type) {
        // ignore the parameter
        return Money.ZERO(this.currency);
    }


    public TransactionId getOriginalTransactionId() {
        return originalTransactionId;
    }


    public ReversalReason getReason() {
        return reason;
    }


    public ReversalType getReversalType() {
        return reversalType;
    }


    public Currency getCurrency() {
        return currency;
    }


    public String getInitiatedBy() {
        return initiatedBy;
    }


    public Instant getReversedAt() {
        return reversedAt;
    }


    public String getNote() {
        return note;
    }

    
    

    
}
