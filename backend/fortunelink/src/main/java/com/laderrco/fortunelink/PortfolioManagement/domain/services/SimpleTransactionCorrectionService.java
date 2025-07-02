package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Transaction;

public class SimpleTransactionCorrectionService implements TransactionCorrectionService {

    @Override
    public void applyCompensationForVoidedTransaction(Portfolio portfolio, Transaction originalTransaction,
            String reason) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyCompensationForVoidedTransaction'");
    }
    
}
