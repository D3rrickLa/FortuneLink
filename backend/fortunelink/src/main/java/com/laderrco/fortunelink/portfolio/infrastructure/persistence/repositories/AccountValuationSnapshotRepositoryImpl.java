package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.AccountValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.repositories.AccountValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountValuationSnapshotJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.AccountValuationSnapshotDomainMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AccountValuationSnapshotRepositoryImpl implements AccountValuationSnapshotRepository {
  private final JpaAccountValuationSnapshotRepository jpaRepo;

  @Override
  public AccountValuationSnapshot save(AccountValuationSnapshot snapshot) {
    UUID accountId = snapshot.accountId().id();
    LocalDate date = snapshot.snapshotDay();

    Optional<AccountValuationSnapshotJpaEntity> existing = jpaRepo.findByAccountIdAndSnapshotDayEquals(accountId, date);

    AccountValuationSnapshotJpaEntity entity;

    if (existing.isPresent()) {
      entity = AccountValuationSnapshotDomainMapper.updateJpa(existing.get(), snapshot);
    } else {
      entity = AccountValuationSnapshotDomainMapper.toJpa(UUID.randomUUID(), snapshot);
    }

    AccountValuationSnapshotJpaEntity saved = jpaRepo.save(entity);
    return AccountValuationSnapshotDomainMapper.toDomain(saved);
  }

  @Override
  public List<AccountValuationSnapshot> findByAccountIdAndSnapshotDayAfterOrderBySnapshotDayAsc(
      AccountId accountId, LocalDate after) {

    return jpaRepo
        .findByAccountIdAndSnapshotDayAfterOrderBySnapshotDayAsc(
            accountId.id(),
            after)
        .stream()
        .map(AccountValuationSnapshotDomainMapper::toDomain)
        .toList();
  }

  @Override
  public Optional<AccountValuationSnapshot> findByAccountIdAndSnapshotDay(AccountId accountId, LocalDate date) {
    return jpaRepo.findByAccountIdAndSnapshotDayEquals(accountId.id(), date)
        .map(AccountValuationSnapshotDomainMapper::toDomain);
  }

}