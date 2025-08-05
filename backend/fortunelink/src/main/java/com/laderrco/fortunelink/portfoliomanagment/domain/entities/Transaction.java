package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.TransactionStatus;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;

public class Transaction {
    private final TransactionId transactionId;
    private final CorrelationId correlationId; // for when an event generates multiple transactions
    private final TransactionId parentTransactionId;
    
    private final TransactionType type;
    private TransactionStatus status;


    private final Instant createdAt;
    private final Instant updatedAt;


}
