package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Transaction;

public interface TransactionCorrectionService {
    public void applyCompensationForVoidedTransaction(Portfolio portfolio, Transaction originalTransaction,String reason);

}
