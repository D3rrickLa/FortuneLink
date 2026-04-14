package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.NetWorthSnapshotJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNetWorthSnapshotRepository extends
    JpaRepository<NetWorthSnapshotJpaEntity, UUID> {

  /**
   * Snapshots in ascending date order for chart rendering (oldest → newest). Caller determines the
   * window , typically 90 or 365 days.
   */
  @Query("""
      SELECT s FROM NetWorthSnapshotJpaEntity s
      WHERE s.userId = :userId
        AND s.snapshotDate >= :since
      ORDER BY s.snapshotDate ASC
      """)
  List<NetWorthSnapshotJpaEntity> findByUserIdSince(@Param("userId") UUID userId,
      @Param("since") Instant since);

  /**
   * Day-boundary check. NEVER use DATE() in JPQL against a TIMESTAMPTZ column , the cast behavior
   * is JVM-timezone-dependent. Instead pass explicit UTC bounds computed by the caller (start of
   * today UTC, start of tomorrow UTC).
   *
   * <p>
   * The caller in {@code NetWorthSnapshotRepositoryImpl.existsForToday()} computes these bounds and
   * passes them here.
   */
  @Query("""
      SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
      FROM NetWorthSnapshotJpaEntity s
      WHERE s.userId = :userId
        AND s.snapshotDate >= :startOfDay
        AND s.snapshotDate < :endOfDay
      """)
  boolean existsBetween(@Param("userId") UUID userId, @Param("startOfDay") Instant startOfDay,
      @Param("endOfDay") Instant endOfDay);
}
