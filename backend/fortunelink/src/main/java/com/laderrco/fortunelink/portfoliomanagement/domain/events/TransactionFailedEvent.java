package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;

public class TransactionFailedEvent implements DomainEvent {

    public TransactionFailedEvent(TransactionId transactionId, PortfolioId portfolioId, String reason) {
        //TODO Auto-generated constructor stub
    }

    @Override
    public Instant occuredAt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'occuredAt'");
    }
    
}
