package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.ValuationSnapshotJpaEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ValuationSnapshotRepositoryImpl implements ValuationSnapshotRepository {
  private final JpaValuationSnapshotRepository jpaRepository;

  @Override
  public void save(ValuationSnapshot snapshot) {
    jpaRepository.save(ValuationSnapshotJpaEntity.from(snapshot));
  }

  @Override
  public List<ValuationSnapshot> findByUserIdSince(UserId userId, Instant since) {
    return jpaRepository.findByUserIdSince(UUID.fromString(userId.toString()), since).stream()
        .map(ValuationSnapshotJpaEntity::toDomain).toList();
  }

  /**
   * Computes UTC day boundaries in the impl rather than using DATE() in JPQL. DATE() behavior in
   * JPQL is JVM-timezone-dependent , on a UTC container this is fine, but it's an invisible trap
   * when someone runs the app locally with a non-UTC system timezone. Explicit bounds are
   * unambiguous.
   */
  @Override
  public boolean existsForToday(UserId userId) {
    Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

    return jpaRepository.existsBetween(UUID.fromString(userId.toString()), startOfDay, endOfDay);
  }
}