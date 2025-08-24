package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;

public abstract class TransactionDetails {
    private final TransactionSource source;
    private final String description;
    private final List<Fee> fees;

    protected TransactionDetails(
        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
       this.source = Objects.requireNonNull(source, "Source cannot be null.");; 
       this.description = description.trim();
       this.fees = fees != null ? Collections.unmodifiableList(fees) : Collections.emptyList();
    }

    public TransactionSource getSource() {return source;}
    public String getDescription() {return description;}
    public List<Fee> getFees() {return fees;}
}
