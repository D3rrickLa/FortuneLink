package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// fine-grained, business-level classification
// this is defined in the Transaction.java class
public interface TransactionType {
    String getCode();
    TransactionCategory getCategory();
}