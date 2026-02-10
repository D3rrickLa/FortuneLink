package com.laderrco.fortunelink.portfolio_management.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(TransactionId transactionId);
    List<Transaction> findByAccountId(AccountId accountId);
    List<Transaction> findByDateRange(AccountId accountId, Instant start, Instant end);
    void delete(TransactionId transactionId);
}