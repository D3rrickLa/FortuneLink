package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionStatus;

public class InvalidTransactionStateException extends RuntimeException {
    public InvalidTransactionStateException(String s) {
        super(s);
    }
    
    public InvalidTransactionStateException(TransactionStatus from, TransactionStatus to) {
        super(String.format("Invalid transition from %s to %s", from, to));
    }
}