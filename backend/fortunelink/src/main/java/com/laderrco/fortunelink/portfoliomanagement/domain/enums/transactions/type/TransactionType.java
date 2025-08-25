package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// fine-grained, business-level classification
public interface TransactionType {
    String getCode();
    TransactionCategory getCategory();
}