package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.Collections;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;

public abstract class TransactionDetails {
    private final TransactionSource source;
    private final String description;
    private final List<Fee> fees;

    
    protected TransactionDetails(
        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        this.source = source;
        this.description = description.trim();
        this.fees = fees != null ? Collections.unmodifiableList(fees) : Collections.emptyList();

    }

    public TransactionSource getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public List<Fee> getFees() {
        return fees;
    }  
}
