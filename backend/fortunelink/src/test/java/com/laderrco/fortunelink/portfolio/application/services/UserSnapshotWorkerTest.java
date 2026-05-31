package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.AccountValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Snapshot Worker Unit Tests")
class UserSnapshotWorkerTest {

  private final UserId USER_ID = UserId.random();
  private final Currency USD = Currency.of("USD");

  @Mock
  private MarketDataService marketDataService;
  @Mock
  private PortfolioRepository portfolioRepository;
  @Mock
  private AccountValuationSnapshotRepository accountSnapshotRepository;
  @Mock
  private ValuationSnapshotRepository snapshotRepository;
  @Mock
  private PortfolioValuationService valuationService;

  @InjectMocks
  private UserSnapshotWorker snapshotWorker;

  // =====================================================
  // 1. Quote branching test
  // =====================================================

  @Test
  @DisplayName("snapshotForUser: tests quote fetch branching (empty vs non-empty symbols)")
  void snapshotForUserQuoteFetchingBranching() {

    Portfolio p = mock(Portfolio.class);
    AssetSymbol apple = new AssetSymbol("AAPL");

    ValuationView viewUSD = new ValuationView(
        Money.of("0.00", USD),
        Money.zero(USD),
        Money.zero(USD),
        BigDecimal.ZERO,
        Money.zero(USD),
        Money.zero(USD),
        USD,
        false,
        Instant.now());

    when(portfolioRepository.findAllActiveByUserId(USER_ID))
        .thenReturn(List.of(p));

    when(p.getDisplayCurrency()).thenReturn(USD);

    when(valuationService.calculateUserValuation(anyList(), eq(USD), anyMap()))
        .thenReturn(viewUSD);

    try (MockedStatic<PortfolioAccessUtils> utils = Mockito.mockStatic(PortfolioAccessUtils.class)) {

      // -------------------------
      // CASE 1: EMPTY SYMBOLS
      // -------------------------
      utils.when(() -> PortfolioAccessUtils.extractSymbols(p))
          .thenReturn(Set.of());

      snapshotWorker.snapshotForUser(USER_ID);

      verify(marketDataService, never()).getBatchQuotes(any());

      clearInvocations(marketDataService);

      // -------------------------
      // CASE 2: SYMBOLS PRESENT
      // -------------------------
      utils.when(() -> PortfolioAccessUtils.extractSymbols(p))
          .thenReturn(Set.of(apple));

      when(marketDataService.getBatchQuotes(Set.of(apple)))
          .thenReturn(Map.of());

      snapshotWorker.snapshotForUser(USER_ID);

      verify(marketDataService, times(1))
          .getBatchQuotes(Set.of(apple));
    }
  }

  // =====================================================
  // 2. No-op cases
  // =====================================================

  @Test
  @DisplayName("snapshotForUser: returns false when no portfolios exist")
  void returnsFalseWhenNoPortfolios() {

    when(portfolioRepository.findAllActiveByUserId(USER_ID))
        .thenReturn(List.of());

    assertThat(snapshotWorker.snapshotForUser(USER_ID))
        .isFalse();
  }

  // =====================================================
  // 3. Success path
  // =====================================================

  @Test
  @DisplayName("snapshotForUser: full success path")
  void createsSnapshotSuccessfully() {

    Portfolio p = mock(Portfolio.class);
    Account a = mock(Account.class);

    ValuationView viewUSD = new ValuationView(
        Money.of("100.00", USD),
        Money.zero(USD),
        Money.zero(USD),
        BigDecimal.ZERO,
        Money.zero(USD),
        Money.zero(USD),
        USD,
        true,
        Instant.now());

    when(portfolioRepository.findAllActiveByUserId(USER_ID))
        .thenReturn(List.of(p));

    when(p.getDisplayCurrency()).thenReturn(USD);
    when(p.getAccounts()).thenReturn(List.of(a));

    when(valuationService.calculateUserValuation(anyList(), eq(USD), anyMap()))
        .thenReturn(viewUSD);

    when(snapshotRepository.findByUserIdAndSnapshotDate(any(), any()))
        .thenReturn(Optional.empty());

    // when(accountSnapshotRepository.findByAccountIdAndSnapshotDate(any(), any()))
    //     .thenReturn(Optional.empty());

    boolean result = snapshotWorker.snapshotForUser(USER_ID);

    assertThat(result).isTrue();

    verify(snapshotRepository).save(argThat(s -> s.userId().equals(USER_ID) &&
        s.hasStaleData()));
  }

  // =====================================================
  // 4. Empty symbols + save still happens
  // =====================================================

  @Test
  @DisplayName("snapshotForUser: handles empty symbols list")
  void handlesEmptySymbols() {

    Portfolio p = mock(Portfolio.class);

    ValuationView viewUSD = new ValuationView(
        Money.of("0.00", USD),
        Money.zero(USD),
        Money.zero(USD),
        BigDecimal.ZERO,
        Money.zero(USD),
        Money.zero(USD),
        USD,
        false,
        Instant.now());

    when(portfolioRepository.findAllActiveByUserId(USER_ID))
        .thenReturn(List.of(p));

    when(p.getDisplayCurrency()).thenReturn(USD);

    when(valuationService.calculateUserValuation(anyList(), eq(USD), anyMap()))
        .thenReturn(viewUSD);

    when(snapshotRepository.findByUserIdAndSnapshotDate(any(), any()))
        .thenReturn(Optional.empty());

    // when(accountSnapshotRepository.findByAccountIdAndSnapshotDate(any(), any()))
    //     .thenReturn(Optional.empty());

    snapshotWorker.snapshotForUser(USER_ID);

    verify(marketDataService, never()).getBatchQuotes(any());
    verify(snapshotRepository).save(any());
  }
}