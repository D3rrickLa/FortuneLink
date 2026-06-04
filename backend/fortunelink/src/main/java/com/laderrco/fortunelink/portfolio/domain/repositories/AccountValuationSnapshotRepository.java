package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.AccountValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountValuationSnapshotRepository {

  AccountValuationSnapshot save(AccountValuationSnapshot snapshot);

  Optional<AccountValuationSnapshot> findByAccountIdAndSnapshotDay(AccountId accountId,
      LocalDate date);

  List<AccountValuationSnapshot> findByAccountIdAndSnapshotDayAfterOrderBySnapshotDayAsc(
      AccountId accountId, LocalDate after);
}