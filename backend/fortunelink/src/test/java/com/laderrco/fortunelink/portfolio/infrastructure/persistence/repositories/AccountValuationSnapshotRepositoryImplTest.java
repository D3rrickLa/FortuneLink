package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.AccountValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountValuationSnapshotJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.AccountValuationSnapshotDomainMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountValuationSnapshotRepositoryImplTest {
  private static final Currency USD = Currency.USD;

  @Mock
  private JpaAccountValuationSnapshotRepository jpaRepo;

  @InjectMocks
  private AccountValuationSnapshotRepositoryImpl repository;

  private final AccountId accountId = new AccountId(UUID.randomUUID());
  private final LocalDate targetDate = LocalDate.of(2026, 5, 28);

  @Test
  @DisplayName("save: converts domain to entity, saves to database, and maps back to domain")
  void saveConvertsAndDelegatesToJpa() {
    // Arrange
    AccountValuationSnapshot domainInput = mock(AccountValuationSnapshot.class);
    when(domainInput.accountId()).thenReturn(accountId);
    when(domainInput.snapshotDay()).thenReturn(targetDate);
    when(domainInput.totalValue()).thenReturn(Money.of(200, Currency.USD));
    when(domainInput.totalCostBasis()).thenReturn(Money.of(100, Currency.USD));
    when(domainInput.unrealizedGainLoss()).thenReturn(Money.of(100, Currency.USD));
    when(domainInput.gainLossPercent()).thenReturn(PercentageChange.ZERO);
    when(domainInput.cashBalance()).thenReturn(Money.of(100, Currency.USD));
    when(domainInput.investedValue()).thenReturn(Money.of(100, Currency.USD));
    when(domainInput.hasStaleData()).thenReturn(false);

    AccountValuationSnapshotJpaEntity jpaSavedEntity = AccountValuationSnapshotDomainMapper.toJpa(UUID.randomUUID(),
        domainInput);

    when(jpaRepo.save(any(AccountValuationSnapshotJpaEntity.class))).thenReturn(jpaSavedEntity);

    AccountValuationSnapshot result = repository.save(domainInput);

    verify(jpaRepo, times(1)).save(any(AccountValuationSnapshotJpaEntity.class));
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("save: delegates to JPA and preserves snapshot identity semantics")
  void saveDelegatesCorrectly() {

    AccountValuationSnapshot domain = new AccountValuationSnapshot(
        accountId,
        targetDate,
        Money.of(100, USD),
        Money.of(50, USD),
        Money.of(50, USD),
        PercentageChange.ZERO,
        Money.of(20, USD),
        Money.of(80, USD),
        false);

    when(jpaRepo.save(any(AccountValuationSnapshotJpaEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountValuationSnapshot result = repository.save(domain);

    verify(jpaRepo).save(any(AccountValuationSnapshotJpaEntity.class));
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc: unwraps domain ID and maps returned stream")
  void findByAccountAndDateAfterDelegatesAndMapsList() {
    // Arrange
    AccountValuationSnapshotJpaEntity entity1 = new AccountValuationSnapshotJpaEntity();
    entity1.setId(UUID.randomUUID());
    entity1.setAccountId(accountId.id());
    entity1.setSnapshotDay(targetDate);
    entity1.setTotalValue(new BigDecimal("100.00"));
    entity1.setTotalCostBasis(new BigDecimal("0.00"));
    entity1.setUnrealizedGainLoss(BigDecimal.ZERO);
    entity1.setGainLossPercent(BigDecimal.ZERO);
    entity1.setCashBalance(new BigDecimal("20.00"));
    entity1.setInvestedValue(new BigDecimal("80.00"));
    entity1.setCurrency("USD");
    entity1.setHasStaleData(false);
    // add cost basis / other fields if required by your constructor

    AccountValuationSnapshotJpaEntity entity2 = new AccountValuationSnapshotJpaEntity();
    entity2.setId(UUID.randomUUID());
    entity2.setAccountId(accountId.id());
    entity2.setSnapshotDay(targetDate);
    entity2.setTotalValue(new BigDecimal("200.00"));
    entity2.setTotalCostBasis(new BigDecimal("0.00"));
    entity2.setUnrealizedGainLoss(BigDecimal.ZERO);
    entity2.setGainLossPercent(BigDecimal.ZERO);
    entity2.setCashBalance(new BigDecimal("50.00"));
    entity2.setInvestedValue(new BigDecimal("150.00"));
    entity2.setCurrency("USD");
    entity2.setHasStaleData(false);

    when(jpaRepo.findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(accountId.id(), targetDate))
        .thenReturn(List.of(entity1, entity2));

    // Act
    List<AccountValuationSnapshot> result = repository
        .findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(accountId, targetDate);

    // Assert
    assertThat(result)
        .isNotNull()
        .hasSize(2);

    verify(jpaRepo).findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(accountId.id(), targetDate);
  }

  @Test
  void findByAccountIdAndSnapshotDate_mapsCorrectly() {
    AccountValuationSnapshotJpaEntity entity = new AccountValuationSnapshotJpaEntity();
    entity.setId(UUID.randomUUID());
    entity.setAccountId(accountId.id());
    entity.setSnapshotDay(targetDate);
    entity.setTotalValue(new BigDecimal("100.00"));
    entity.setTotalCostBasis(new BigDecimal("50.00"));
    entity.setUnrealizedGainLoss(new BigDecimal("50.00"));
    entity.setGainLossPercent(BigDecimal.ZERO);
    entity.setCashBalance(new BigDecimal("20.00"));
    entity.setInvestedValue(new BigDecimal("80.00"));
    entity.setCurrency("USD");
    entity.setHasStaleData(false);

    when(jpaRepo.findByAccountIdAndSnapshotDate(accountId.id(), targetDate))
        .thenReturn(Optional.of(entity));

    Optional<AccountValuationSnapshot> result = repository.findByAccountIdAndSnapshotDate(accountId, targetDate);

    assertThat(result).isPresent();
    verify(jpaRepo).findByAccountIdAndSnapshotDate(accountId.id(), targetDate);
  }
}