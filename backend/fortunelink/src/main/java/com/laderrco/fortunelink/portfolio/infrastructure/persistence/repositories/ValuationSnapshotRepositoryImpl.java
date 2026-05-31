package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.ValuationSnapshotJpaEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ValuationSnapshotRepositoryImpl implements ValuationSnapshotRepository {
  private final JpaValuationSnapshotRepository jpaRepository;

  @Override
  public ValuationSnapshot save(ValuationSnapshot snapshot) {
    jpaRepository.save(ValuationSnapshotJpaEntity.from(snapshot));
    return snapshot;
  }

  @Override
  public List<ValuationSnapshot> findByUserIdSince(UserId userId, Instant since) {
    return jpaRepository.findByUserIdSince(UUID.fromString(userId.toString()), since).stream()
        .map(ValuationSnapshotJpaEntity::toDomain).toList();
  }

  @Override
  public Optional<ValuationSnapshot> findByUserIdAndSnapshotDate(UserId userId, LocalDate date) {
    Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant endOfDay = date.plusDays(1)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant();

    return jpaRepository
        .findByUserIdAndSnapshotDate(
            UUID.fromString(userId.toString()),
            startOfDay,
            endOfDay)
        .map(ValuationSnapshotJpaEntity::toDomain);
  }
}