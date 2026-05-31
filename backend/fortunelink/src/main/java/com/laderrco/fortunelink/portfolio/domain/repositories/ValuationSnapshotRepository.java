package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain port for valuation snapshot persistence.
 *
 * A snapshot represents the latest known valuation for a user on a given UTC day.
 *
 * At most one snapshot exists per user per day.
 * Implementations should support upsert semantics:
 *
 * - create a snapshot if none exists
 * - update the snapshot if one already exists
 */
public interface ValuationSnapshotRepository {

  ValuationSnapshot save(ValuationSnapshot snapshot);

  List<ValuationSnapshot> findByUserIdSince(UserId userId, Instant since);

  Optional<ValuationSnapshot> findByUserIdAndSnapshotDate(UserId userId, LocalDate date);
}