package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(TransactionId transactionId) {
        super("Transaction not found with id: " + transactionId.id().toString());
    }

}
