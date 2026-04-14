package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.List;

/**
 * Domain port for net worth snapshot persistence.
 * <p>
 * Snapshots are append-only. There is intentionally no update or delete operation , the DB unique
 * index enforces one snapshot per user per day, and the snapshot service uses
 * {@code existsForToday} to skip duplicate runs.
 */
public interface NetWorthSnapshotRepository {

  /**
   * Persists a new snapshot. Will throw a DataIntegrityViolationException if a snapshot already
   * exists for this user on the same calendar day (UTC). Callers should check
   * {@link #existsForToday(UserId)} first.
   */
  void save(NetWorthSnapshot snapshot);

  /**
   * Returns all snapshots for a user on or after {@code since}, ordered ascending by snapshotDate.
   * Used to render the FIRE progress chart.
   */
  List<NetWorthSnapshot> findByUserIdSince(UserId userId, Instant since);

  /**
   * Returns true if a snapshot already exists for today (UTC calendar day). Used by the scheduled
   * job to skip double-execution within the same day.
   */
  boolean existsForToday(UserId userId);
}