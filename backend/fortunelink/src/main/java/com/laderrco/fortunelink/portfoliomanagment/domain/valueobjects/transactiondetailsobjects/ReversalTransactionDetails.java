package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;

public final class ReversalTransactionDetails extends TransactionDetails {
    private final TransactionId transactionId;
    private final String reason;
    
    public ReversalTransactionDetails(
        TransactionId transactionId, 
        String reason,

        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        super(source, description, fees);
        this.transactionId = transactionId;
        this.reason = reason;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public String getReason() {
        return reason;
    }
    
}
