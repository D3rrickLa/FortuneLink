package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTransactionRepository extends JpaRepository<Transaction, UUID> {
  @Modifying
  @Query(
      "DELETE FROM Transaction t " + "WHERE t.account.id = :accountId " + "AND t.excluded = true "
          + "AND t.metadata.excludedAt < :cutoff")
  int deleteExpiredTransactions(@Param("accountId") AccountId accountId,
      @Param("cutoff") Instant cutoff);
}
