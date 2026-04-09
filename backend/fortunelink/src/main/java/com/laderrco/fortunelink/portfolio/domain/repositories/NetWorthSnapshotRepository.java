package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public interface NetWorthSnapshotRepository {
  void save(NetWorthSnapshot snapshot);

  // Returns snapshots in ascending date order — callers build the chart from left
  // to right
  List<NetWorthSnapshot> findByUserIdSince(UserId userId, Instant since);

  boolean existsForToday(UserId userId);
}