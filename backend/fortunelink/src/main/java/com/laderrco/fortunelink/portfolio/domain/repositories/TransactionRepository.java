package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    /*
    @Modifying
    @Query("DELETE FROM Transaction t " +
           "WHERE t.account.id = :accountId " +
           "AND t.excluded = true " +
           "AND t.metadata.excludedAt < :cutoff")
    int deleteExpiredTransactions(
        @Param("accountId") Long accountId,
        @Param("cutoff") Instant cutoff
    );
     */
    int deleteExpiredTransactions(AccountId accountId, Instant cutoff);

    Optional<Transaction> findById(TransactionId transactionId);

    Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id, PortfolioId portfolioId, UserId userId, AccountId accountId);

    List<Transaction> findByAccountId(AccountId accountId);

    List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol);

    List<Transaction> findByDateRange(AccountId accountId, Instant start, Instant end);

    int deleteAllExpiredTransactions(Instant cutoff);
}