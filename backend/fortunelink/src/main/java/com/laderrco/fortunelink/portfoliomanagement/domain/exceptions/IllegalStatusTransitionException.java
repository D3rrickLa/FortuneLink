package com.laderrco.fortunelink.portfoliomanagement.domain.exceptions;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionStatus;

public class IllegalStatusTransitionException extends RuntimeException{
    public IllegalStatusTransitionException(TransactionStatus status, TransactionStatus newStatus) {
        super(String.format("%s cannot be applied to transaction when %s status is in effect", newStatus.toString(), status.toString()));
    }
}
