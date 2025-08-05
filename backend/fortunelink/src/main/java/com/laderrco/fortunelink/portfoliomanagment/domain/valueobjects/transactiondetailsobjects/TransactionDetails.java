package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionStatus;

// NOTE: some of the variables are lifecycle items, please remove and place them into Transaction.java
// Remember: VO -> immutable objects whose identity is defined by its data while Entities can change over time
public abstract class TransactionDetails {
    // put common transaction inputs in here
    // metadata will be in this class rather than a separate class
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


    public abstract TransactionDetails updateStatus(TransactionStatus newStatus, Instant updatedAt);

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
