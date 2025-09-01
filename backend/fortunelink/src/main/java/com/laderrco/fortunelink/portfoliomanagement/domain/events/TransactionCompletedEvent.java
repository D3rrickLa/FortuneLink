package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;

public class TransactionCompletedEvent implements DomainEvent {

    public TransactionCompletedEvent(TransactionId transactionId, PortfolioId portfolioId, TransactionType type) {
        //TODO Auto-generated constructor stub
    }

    @Override
    public Instant occuredAt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'occuredAt'");
    }
    
}
