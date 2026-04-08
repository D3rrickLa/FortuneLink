package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.application.repositories.RealizedGainsQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.GainsAggregation;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.RealizedGainJpaEntity;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealizedGainsQueryRepositoryImpl implements RealizedGainsQueryRepository {
  private final JpaRealizedGainRepository jpaRepository;

  @Override
  public Page<RealizedGainRecord> findByAccountId(AccountId accountId, Pageable pageable) {
    return jpaRepository.findByAccountId(uuid(accountId), pageable).map(this::toDomain);
  }

  @Override
  public Page<RealizedGainRecord> findByAccountIdAndYear(AccountId accountId, int year, Pageable pageable) {
    return jpaRepository.findByAccountIdAndYear(uuid(accountId), year, pageable).map(this::toDomain);
  }

  @Override
  public Page<RealizedGainRecord> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol, Pageable pageable) {
    return jpaRepository.findByAccountIdAndSymbol(uuid(accountId), symbol.symbol(), pageable).map(this::toDomain);
  }

  @Override
  public Page<RealizedGainRecord> findByAccountIdAndYearAndSymbol(AccountId accountId, int year, AssetSymbol symbol,
      Pageable pageable) {
    return jpaRepository.findByAccountIdAndYearAndSymbol(uuid(accountId), year, symbol.symbol(), pageable)
        .map(this::toDomain);
  }

  @Override
  public GainsAggregation calculateTotals(AccountId accountId, Integer taxYear, AssetSymbol symbol) {
    // Implementation logic for calculating totals via jpaRepository
    // This should call the @Query method defined in your JpaRealizedGainRepository
    String symbolStr = (symbol != null) ? symbol.symbol() : null;
    return jpaRepository.calculateTotals(uuid(accountId), taxYear, symbolStr);
  }

  @Override
  public Optional<String> findAccountCurrencyCode(AccountId accountId) {
    return jpaRepository.findAccountCurrencyById(uuid(accountId));
  }

  private RealizedGainRecord toDomain(RealizedGainJpaEntity e) {
    Currency currency = Currency.of(e.getGainLossCurrency());
    return RealizedGainRecord.reconstitute(e.getId(), new AssetSymbol(e.getSymbol()),
        new Money(e.getGainLossAmount(), currency),new Money(e.getCostBasisSoldAmount(), 
        Currency.of(e.getCostBasisSoldCurrency())), e.getOccurredAt());
  }

  private UUID uuid(AccountId id) {
    return UUID.fromString(id.toString());
  }
}