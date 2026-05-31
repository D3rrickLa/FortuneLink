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
    AccountValuationSnapshotJpaEntity entity = AccountValuationSnapshotDomainMapper.toJpa(UUID.randomUUID(), snapshot);
    AccountValuationSnapshotJpaEntity saved = jpaRepo.save(entity);
    return AccountValuationSnapshotDomainMapper.toDomain(saved);
  }

  @Override
  public List<AccountValuationSnapshot> findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(
      AccountId accountId,
      LocalDate after) {

    return jpaRepo
        .findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(
            accountId.id(),
            after)
        .stream()
        .map(AccountValuationSnapshotDomainMapper::toDomain)
        .toList();
  }

  @Override
  public Optional<AccountValuationSnapshot> findByAccountIdAndSnapshotDate(AccountId accountId, LocalDate date) {
    return jpaRepo.findByAccountIdAndSnapshotDate(accountId.id(), date)
        .map(AccountValuationSnapshotDomainMapper::toDomain);
  }

}