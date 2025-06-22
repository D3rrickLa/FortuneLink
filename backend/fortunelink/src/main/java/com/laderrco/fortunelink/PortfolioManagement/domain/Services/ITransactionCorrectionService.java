package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Portfolio;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Transaction;

public interface ITransactionCorrectionService {
    // we moved the voiding of transaction to here, a service, because it is easier to maintain
    // and it's too complex for the aggregate
    public void applyCompensationForVoidedTransaction(Portfolio portfolio, Transaction originalTransaction, String reason);
}
