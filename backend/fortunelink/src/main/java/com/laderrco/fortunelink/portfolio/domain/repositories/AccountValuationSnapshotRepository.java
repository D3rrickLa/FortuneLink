package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.time.LocalDate;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.AccountValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

public interface AccountValuationSnapshotRepository {
  AccountValuationSnapshot save(AccountValuationSnapshot snapshot);
  
  boolean existsByAccountIdAndSnapshotDate(AccountId accountId, LocalDate date);

  List<AccountValuationSnapshot> findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(
      AccountId accountId, LocalDate after);
}
