package com.laderrco.fortunelink.PortfolioManagement.domain.Repositories;

import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Transaction;

// we implement this in the Infra layer
public interface ITransactionRepository {
    public Optional<Transaction> findById(UUID id);
    public Transaction saveTransaction(Transaction transaction);
}
