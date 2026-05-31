package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.AccountValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

public interface AccountValuationSnapshotRepository {

  AccountValuationSnapshot save(AccountValuationSnapshot snapshot);

  Optional<AccountValuationSnapshot> findByAccountIdAndSnapshotDate(
      AccountId accountId,
      LocalDate date);

  List<AccountValuationSnapshot> findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(
      AccountId accountId,
      LocalDate after);
}