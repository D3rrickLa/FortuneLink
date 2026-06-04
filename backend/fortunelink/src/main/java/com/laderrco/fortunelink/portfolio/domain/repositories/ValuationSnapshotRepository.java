package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain port for valuation snapshot persistence.
 * <p>
 * A snapshot represents the latest known valuation for a user on a given UTC day.
 * <p>
 * At most one snapshot exists per user per day. Implementations should support upsert semantics:
 * <p>
 * - create a snapshot if none exists - update the snapshot if one already exists
 */
public interface ValuationSnapshotRepository {

  ValuationSnapshot save(ValuationSnapshot snapshot);

  List<ValuationSnapshot> findByUserIdSince(UserId userId, LocalDate since);

  Optional<ValuationSnapshot> findByUserIdAndSnapshotDay(UserId userId, LocalDate date);
}