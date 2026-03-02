package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    // this method, be careful, only call this when the 'excluded' has been sitting for a while or user says for real delete
    void delete(TransactionId transactionId);

    Optional<Transaction> findById(TransactionId transactionId);

    Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id, PortfolioId portfolioId, UserId userId, AccountId accountId);

    List<Transaction> findByAccountId(AccountId accountId);

    List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol);

    List<Transaction> findByDateRange(AccountId accountId, Instant start, Instant end);

}