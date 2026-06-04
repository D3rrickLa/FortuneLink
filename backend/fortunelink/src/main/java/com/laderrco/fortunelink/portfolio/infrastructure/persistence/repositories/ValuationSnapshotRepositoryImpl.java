package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.ValuationSnapshotJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ValuationSnapshotRepositoryImpl implements ValuationSnapshotRepository {
  private final JpaValuationSnapshotRepository jpaRepository;

  @Override
  public ValuationSnapshot save(ValuationSnapshot snapshot) {
    ValuationSnapshotJpaEntity entity = ValuationSnapshotJpaEntity.from(snapshot);

    jpaRepository.upsertSnapshot(entity.getId(), entity.getUserId(), entity.getTotalValueAmount(),
        entity.getTotalValueCurrency(), entity.getTotalCostBasisAmount(),
        entity.getTotalCostBasisCurrency(), entity.getUnrealizedGainLossAmount(),
        entity.getUnrealizedGainLossCurrency(), entity.getGainLossPercent(),
        entity.getTotalCashBalanceAmount(), entity.getTotalCashBalanceCurrency(),
        entity.getTotalInvestedValueAmount(), entity.getTotalInvestedValueCurrency(),
        entity.getDisplayCurrencyCode(), entity.isHasStaleData(), entity.getSnapshotDay(),
        entity.getCreatedAt());

    return snapshot;
  }

  @Override
  public List<ValuationSnapshot> findByUserIdSince(UserId userId, LocalDate since) {
    return jpaRepository.findByUserIdSince(UUID.fromString(userId.toString()), since).stream()
        .map(ValuationSnapshotJpaEntity::toDomain).toList();
  }

  @Override
  public Optional<ValuationSnapshot> findByUserIdAndSnapshotDay(UserId userId, LocalDate date) {
    return jpaRepository.findByUserIdAndSnapshotDay(UUID.fromString(userId.toString()), date)
        .map(ValuationSnapshotJpaEntity::toDomain);
  }
}