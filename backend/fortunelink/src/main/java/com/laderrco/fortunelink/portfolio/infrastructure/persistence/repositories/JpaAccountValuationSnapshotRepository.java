package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountValuationSnapshotJpaEntity;

@Repository
public interface JpaAccountValuationSnapshotRepository extends JpaRepository<AccountValuationSnapshotJpaEntity, UUID> {
  boolean existsByAccountIdAndSnapshotDate(UUID accountId, LocalDate date);

  List<AccountValuationSnapshotJpaEntity> findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(
      UUID accountId, LocalDate after);
}