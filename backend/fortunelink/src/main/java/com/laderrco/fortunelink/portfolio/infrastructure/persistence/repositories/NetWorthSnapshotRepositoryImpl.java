package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.NetWorthSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.NetWorthSnapshotJpaEntity;
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
public class NetWorthSnapshotRepositoryImpl implements NetWorthSnapshotRepository {
  private final JpaNetWorthSnapshotRepository jpaRepository;

  @Override
  public void save(NetWorthSnapshot snapshot) {
    jpaRepository.save(NetWorthSnapshotJpaEntity.from(snapshot));
  }

  @Override
  public List<NetWorthSnapshot> findByUserIdSince(UserId userId, Instant since) {
    return jpaRepository.findByUserIdSince(UUID.fromString(userId.toString()), since).stream()
        .map(NetWorthSnapshotJpaEntity::toDomain).toList();
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