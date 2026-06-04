package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountValuationSnapshotJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAccountValuationSnapshotRepository extends
    JpaRepository<AccountValuationSnapshotJpaEntity, UUID> {
  Optional<AccountValuationSnapshotJpaEntity> findByAccountIdAndSnapshotDayBetween(UUID accountId,
      LocalDate startOfDay, LocalDate endOfDay);

  List<AccountValuationSnapshotJpaEntity> findByAccountIdAndSnapshotDayAfterOrderBySnapshotDayAsc(
      UUID accountId, LocalDate after);

  Optional<AccountValuationSnapshotJpaEntity> findByAccountIdAndSnapshotDayEquals(UUID accountId,
      LocalDate snapshotDay);
}