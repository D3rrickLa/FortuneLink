package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.TransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Transaction {
    private final UUID transactionId;
    private final UUID portfolioId;
    private final UUID correlationId; // for events that happen together, like conversion of money or stock splits
    private final UUID parentTransactionId;

    private final TransactionType transactionType;
    private final Money totalTransactionAmount;
    private final Instant transactionDate;
    private final com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.TransactionDetails transactionDetails;
    private final TransactionMetadata transactionMetadata;
    
    private final List<Fee> fees;
    private boolean hidden;
    private int version; // for updating if fixes are needed
    
    
    


    public Transaction(
        UUID transactionId, 
        UUID portfolioId, 
        UUID correlationId,
        UUID parentTransactionId,
        TransactionType transactionType, 
        Money totalTransactionAmount, 
        Instant transactionDate,
        TransactionDetails transactionDetails, 
        TransactionMetadata transactionMetadata, 
        List<Fee> fees,
        boolean hidden, 
        int version
    ) {
        this.transactionId = transactionId;
        this.portfolioId = portfolioId;
        this.correlationId = correlationId;
        this.parentTransactionId = parentTransactionId;
        this.transactionType = transactionType;
        this.totalTransactionAmount = totalTransactionAmount;
        this.transactionDate = transactionDate;
        this.transactionDetails = transactionDetails;
        this.transactionMetadata = transactionMetadata;
        this.fees = fees;
        this.hidden = hidden;
        this.version = version;
    }





    public boolean isReversed() {
        return Set.of(TransactionType.REVERSAL_BUY, TransactionType.REVERSAL_DEPOSIT, TransactionType.REVERSAL_SELL, TransactionType.REVERSAL_WITHDRAWAL).contains(this.transactionType);
    }
    
}
