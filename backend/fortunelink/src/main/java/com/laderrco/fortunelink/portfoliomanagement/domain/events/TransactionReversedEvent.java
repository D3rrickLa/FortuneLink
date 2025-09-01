package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;

public class TransactionReversedEvent implements DomainEvent {

    public TransactionReversedEvent(TransactionId transactionId, PortfolioId portfolioId,
            TransactionId transactionId2) {
        //TODO Auto-generated constructor stub
    }

    @Override
    public Instant occuredAt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'occuredAt'");
    }
    
}
