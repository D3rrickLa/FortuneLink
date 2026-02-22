package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    void delete(TransactionId transactionId);

    Optional<Transaction> findById(TransactionId transactionId);

    Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id, PortfolioId portfolioId, UserId userId, AccountId accountId);

    List<Transaction> findByAccountId(AccountId accountId);

    List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol);

    List<Transaction> findByDateRange(AccountId accountId, Instant start, Instant end);

}