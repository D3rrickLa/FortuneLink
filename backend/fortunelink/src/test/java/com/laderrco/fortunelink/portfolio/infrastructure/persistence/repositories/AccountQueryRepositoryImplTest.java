package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.services.projectors.AssetBalanceProjection;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.PortfolioDomainMapper;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSymbolProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountQueryRepositoryImpl Unit Tests")
class AccountQueryRepositoryImplTest {

  private static final String CAD = "CAD";
  private static final UUID RAW_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID RAW_PORTFOLIO_ID = UUID.randomUUID();
  private static final UUID RAW_USER_ID = UUID.randomUUID();
  private static final AccountId ACCOUNT_ID = AccountId.fromString(RAW_ACCOUNT_ID.toString());
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.fromString(
      RAW_PORTFOLIO_ID.toString());
  private static final UserId USER_ID = UserId.fromString(RAW_USER_ID.toString());
  private static final Pageable PAGEABLE = PageRequest.of(0, 10);
  @Mock
  private JpaAccountRepository jpaAccountRepository;
  @Mock
  private PortfolioDomainMapper mapper;
  @InjectMocks
  private AccountQueryRepositoryImpl repository;

  private PortfolioJpaEntity createPortfolio() {
    return PortfolioJpaEntity.create(RAW_PORTFOLIO_ID, RAW_USER_ID, "Portfolio Name", "Desc", CAD,
        false, null, null, Instant.now(), Instant.now());
  }

  private AccountJpaEntity createAccount() {
    return AccountJpaEntity.create(RAW_ACCOUNT_ID, createPortfolio(), "name", "TFSA", CAD, "ACB",
        "HEALTHY", "ACTIVE", null, CAD, null, Instant.now(), Instant.now());
  }

  private record TestAssetBalanceProjection(
      UUID getAccountId, String getSymbol, BigDecimal getQuantity) implements
      AssetBalanceProjection {
  }

  @Nested
  @DisplayName("findQuantitiesForAccounts Input Validation")
  class InputValidation {

    @Test
    @DisplayName("should return empty map when accountIds is null")
    void findQuantitiesShouldReturnEmptyMapWhenNull() {
      Map<AccountId, Map<AssetSymbol, Quantity>> result = repository.findQuantitiesForAccounts(
          null);

      assertThat(result).isEmpty();
      verifyNoInteractions(jpaAccountRepository);
    }

    @Test
    @DisplayName("should return empty map when accountIds is empty")
    void findQuantitiesShouldReturnEmptyMapWhenEmpty() {
      Map<AccountId, Map<AssetSymbol, Quantity>> result = repository.findQuantitiesForAccounts(
          List.of());

      assertThat(result).isEmpty();
      verifyNoInteractions(jpaAccountRepository);
    }
  }

  @Nested
  @DisplayName("Basic Query Operations")
  class BasicQueries {

    @Test
    @DisplayName("findByPortfolioId should delegate to jpaRepository with UUID")
    void findByPortfolioIdShouldCallJpa() {
      Page<AccountSummaryProjection> expectedPage = new PageImpl<>(List.of());
      when(jpaAccountRepository.findByPortfolioId(RAW_PORTFOLIO_ID, PAGEABLE)).thenReturn(
          expectedPage);

      Page<AccountSummaryProjection> result = repository.findByPortfolioId(PORTFOLIO_ID, PAGEABLE);

      assertThat(result).isEqualTo(expectedPage);
      verify(jpaAccountRepository).findByPortfolioId(RAW_PORTFOLIO_ID, PAGEABLE);
    }

    @Test
    @DisplayName("findByIdWithDetails should use ownership check and map to domain")
    void findByIdWithDetailsShouldMapResult() {
      AccountJpaEntity entity = createAccount();
      Account domainAccount = mock(Account.class);

      when(jpaAccountRepository.findByIdWithOwnershipCheck(RAW_ACCOUNT_ID, RAW_PORTFOLIO_ID,
          RAW_USER_ID)).thenReturn(Optional.of(entity));
      when(mapper.accountToDomain(entity)).thenReturn(domainAccount);

      Optional<Account> result = repository.findByIdWithDetails(ACCOUNT_ID, PORTFOLIO_ID, USER_ID);

      assertThat(result).contains(domainAccount);
    }
  }

  @Nested
  @DisplayName("Batch Grouping Operations")
  class BatchGroupingQueries {

    @Test
    @DisplayName("should correctly group multiple projection rows into nested maps")
    void findQuantitiesShouldGroupRowsCorrectly() {

      AssetBalanceProjection row1 = new TestAssetBalanceProjection(RAW_ACCOUNT_ID, "BTC",
          new BigDecimal("1.5"));
      AssetBalanceProjection row2 = new TestAssetBalanceProjection(RAW_ACCOUNT_ID, "ETH",
          new BigDecimal("10.0"));

      when(jpaAccountRepository.findBalancesForAccounts(anyList())).thenReturn(List.of(row1, row2));

      Map<AccountId, Map<AssetSymbol, Quantity>> result = repository.findQuantitiesForAccounts(
          List.of(ACCOUNT_ID));

      assertThat(result).containsKey(ACCOUNT_ID);
      Map<AssetSymbol, Quantity> balances = result.get(ACCOUNT_ID);

      assertThat(balances).hasSize(2);
      assertThat(balances.get(new AssetSymbol("BTC")).amount()).isEqualByComparingTo("1.5");
      assertThat(balances.get(new AssetSymbol("ETH")).amount()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("findSymbolsForAccounts should group rows into Map per account")
    void findSymbolsForAccountsShouldGroupCorrectly() {

      AccountSymbolProjection row = mock(AccountSymbolProjection.class);
      when(row.getAccountId()).thenReturn(RAW_ACCOUNT_ID);
      when(row.getSymbol()).thenReturn("AAPL");

      when(jpaAccountRepository.findSymbolsForAccounts(anyList())).thenReturn(List.of(row));

      Map<AccountId, Set<AssetSymbol>> result = repository.findSymbolsForAccounts(
          List.of(ACCOUNT_ID));

      assertThat(result).containsKey(ACCOUNT_ID);
      assertThat(result.get(ACCOUNT_ID)).contains(new AssetSymbol("AAPL"));
    }
  }
}