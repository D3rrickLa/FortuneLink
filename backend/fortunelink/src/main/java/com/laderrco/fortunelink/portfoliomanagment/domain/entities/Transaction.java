package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.time.Instant;

import org.springframework.transaction.TransactionStatus;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TransactionDetails;

public class Transaction {
    private final TransactionId transactionId;
    private final CorrelationId correlationId; // for when an event generates multiple transactions
    private final TransactionId parentTransactionId;
    private final PortfolioId portfolioId;
    
    private final TransactionType type;
    private TransactionStatus status;
    private final TransactionDetails transactionDetails;

    private Money transactionNetImpact; // in portfolio's currency
    private final Instant transactionDate;

    private boolean hidden;
    private int version;

    private final Instant createdAt;
    private Instant updatedAt;

    

    public Transaction(
        TransactionId transactionId, 
        CorrelationId correlationId, 
        TransactionId parentTransactionId,
        PortfolioId portfolioId, 
        TransactionType type, 
        TransactionStatus status,
        TransactionDetails transactionDetails, 
        Money transactionNetImpact, 
        Instant transactionDate,
        Instant createdAt
    ) {
        this.transactionId = transactionId;
        this.correlationId = correlationId;
        this.parentTransactionId = parentTransactionId;
        this.portfolioId = portfolioId;
        this.type = type;
        this.status = status;
        this.transactionDetails = transactionDetails;
        this.transactionNetImpact = transactionNetImpact;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;

        this.hidden = false;
        this.version = 1;
    }

    private void updateVersion() {
        this.version += 1;
    } 


    public void updateStatus(TransactionStatus newStatus, Instant updatedAt) {
        this.status = newStatus;
        this.updatedAt = updatedAt;
        updateVersion();
    }

  
}
