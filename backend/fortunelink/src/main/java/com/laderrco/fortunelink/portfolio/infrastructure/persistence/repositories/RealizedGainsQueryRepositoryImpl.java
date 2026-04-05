package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.application.repositories.RealizedGainsQueryRepository;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.RealizedGainJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealizedGainsQueryRepositoryImpl implements RealizedGainsQueryRepository {
  private final JpaRealizedGainRepository jpaRepository;

  @Override
  public List<RealizedGainRecord> findByAccountId(AccountId accountId) {
    return map(jpaRepository.findByAccountId(uuid(accountId)));
  }

  @Override
  public List<RealizedGainRecord> findByAccountIdAndYear(AccountId accountId, int year) {
    return map(jpaRepository.findByAccountIdAndYear(uuid(accountId), year));
  }

  @Override
  public List<RealizedGainRecord> findByAccountIdAndSymbol(AccountId accountId,
      AssetSymbol symbol) {
    return map(jpaRepository.findByAccountIdAndSymbol(uuid(accountId), symbol.symbol()));
  }

  @Override
  public List<RealizedGainRecord> findByAccountIdAndYearAndSymbol(AccountId accountId, int year,
      AssetSymbol symbol) {
    return map(
        jpaRepository.findByAccountIdAndYearAndSymbol(uuid(accountId), year, symbol.symbol()));
  }

  @Override
  public Optional<String> findAccountCurrencyCode(AccountId accountId) {
    return jpaRepository.findAccountCurrencyById(uuid(accountId));
  }

  private List<RealizedGainRecord> map(List<RealizedGainJpaEntity> entities) {
    return entities.stream().map(this::toDomain).toList();
  }

  private RealizedGainRecord toDomain(RealizedGainJpaEntity e) {
    Currency currency = Currency.of(e.getGainLossCurrency());
    return RealizedGainRecord.reconstitute(e.getId(), new AssetSymbol(e.getSymbol()),
        new Money(e.getGainLossAmount(), currency),
        new Money(e.getCostBasisSoldAmount(), Currency.of(e.getCostBasisSoldCurrency())),
        e.getOccurredAt());
  }

  private UUID uuid(AccountId id) {
    return UUID.fromString(id.toString());
  }
}