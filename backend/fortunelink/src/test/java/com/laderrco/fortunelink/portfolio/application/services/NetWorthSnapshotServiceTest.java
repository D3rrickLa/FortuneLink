package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.NetWorthSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Net Worth Snapshot Service Unit Tests")
class NetWorthSnapshotServiceTest {

  @Mock
  private PortfolioRepository portfolioRepository;
  @Mock
  private NetWorthSnapshotRepository snapshotRepository;
  @Mock
  private MarketDataService marketDataService;
  @Mock
  private PortfolioValuationService valuationService;

  @InjectMocks
  private NetWorthSnapshotService snapshotService;

  private final UserId USER_ID = UserId.random();
  private final Currency USD = Currency.of("USD");

  @Test
  @DisplayName("snapshotAllUsers: processes batch and survives individual failures")
  void snapshotAllUsersProcessesBatch() {
    UserId u1 = UserId.random(), u2 = UserId.random();
    when(portfolioRepository.findAllActiveUserIds()).thenReturn(List.of(u1, u2));

    // User 1: Already has snapshot (skip)
    when(snapshotRepository.existsForToday(u1)).thenReturn(true);

    // User 2: Throws error (should be caught by try-catch in service)
    when(snapshotRepository.existsForToday(u2)).thenThrow(new RuntimeException("Fail"));

    snapshotService.snapshotAllUsers();

    verify(snapshotRepository, never()).save(any());
    verify(portfolioRepository, never()).findAllActiveByUserId(any());
  }

  @Nested
  @DisplayName("snapshotForUser Logic")
  class SnapshotLogic {
    @Test
    @DisplayName("snapshotAllUsers: tests success, skipped, and failed counters")
    void snapshotAllUsersBranching() {
      UserId u1 = UserId.random(); // Will succeed (wrote = true)
      UserId u2 = UserId.random(); // Will be skipped (wrote = false)
      UserId u3 = UserId.random(); // Will fail (exception)

      when(portfolioRepository.findAllActiveUserIds()).thenReturn(List.of(u1, u2, u3));

      // User 1 Setup: Has portfolio, no snapshot exists
      when(snapshotRepository.existsForToday(u1)).thenReturn(false);
      Portfolio p = mock(Portfolio.class);
      when(p.getDisplayCurrency()).thenReturn(USD);
      when(portfolioRepository.findAllActiveByUserId(u1)).thenReturn(List.of(p));
      when(valuationService.calculateTotalValue(any(), any(), any())).thenReturn(Money.of(100, "USD"));

      // User 2 Setup: Already has snapshot today (returns false)
      when(snapshotRepository.existsForToday(u2)).thenReturn(true);

      // User 3 Setup: Throws exception
      when(snapshotRepository.existsForToday(u3)).thenThrow(new RuntimeException("Critical failure"));

      snapshotService.snapshotAllUsers();

      // Verify counters logic by checking interactions
      verify(snapshotRepository, times(1)).save(argThat(s -> s.userId().equals(u1)));
      verify(snapshotRepository, times(3)).existsForToday(any());
      // Log info would show success=1, skipped=1, failed=1
    }

    @Test
    @DisplayName("snapshotForUser: tests quote fetch branching (empty symbols vs symbols present)")
    void snapshotForUserQuoteFetchingBranching() {
      Portfolio p = mock(Portfolio.class);
      when(snapshotRepository.existsForToday(USER_ID)).thenReturn(false);
      when(portfolioRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of(p));
      when(p.getDisplayCurrency()).thenReturn(USD);
      when(valuationService.calculateTotalValue(any(), any(), any())).thenReturn(Money.zero(USD));

      // Branch A: No symbols extracted (verify marketDataService is NOT called)
      // Assuming PortfolioAccessUtils.extractSymbols returns empty set for this mock
      snapshotService.snapshotForUser(USER_ID);
      verify(marketDataService, never()).getBatchQuotes(any());

      // Branch B: Symbols present (verify marketDataService IS called)
      // Note: This requires the mock Portfolio to have symbols that
      // PortfolioAccessUtils can find.
      // If PortfolioAccessUtils uses p.getHoldings() or similar, mock those:
      AssetSymbol apple = new AssetSymbol("AAPL");

      // Using a MockedStatic if PortfolioAccessUtils is a static utility
      try (MockedStatic<PortfolioAccessUtils> utils = mockStatic(PortfolioAccessUtils.class)) {
        utils.when(() -> PortfolioAccessUtils.extractSymbols(p)).thenReturn(Set.of(apple));

        snapshotService.snapshotForUser(USER_ID);

        verify(marketDataService, times(1)).getBatchQuotes(Set.of(apple));
      }
    }

    @Test
    @DisplayName("snapshotForUser: returns false when no work needed")
    void returnsFalseWhenNoWork() {
      // Case 1: Already exists
      when(snapshotRepository.existsForToday(USER_ID)).thenReturn(true);
      assertThat(snapshotService.snapshotForUser(USER_ID)).isFalse();

      // Case 2: No portfolios
      when(snapshotRepository.existsForToday(USER_ID)).thenReturn(false);
      when(portfolioRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
      assertThat(snapshotService.snapshotForUser(USER_ID)).isFalse();
    }

    @Test
    @DisplayName("snapshotForUser: full success path with stale data")
    void createsSnapshotSuccessfully() {
      Portfolio p = mock(Portfolio.class);
      Account a = mock(Account.class);

      when(snapshotRepository.existsForToday(USER_ID)).thenReturn(false);
      when(portfolioRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of(p));
      when(p.getDisplayCurrency()).thenReturn(USD);
      when(p.getAccounts()).thenReturn(List.of(a));
      when(a.isStale()).thenReturn(true);

      // Fix: ensure the valuation service returns a real Money object to avoid the
      // NPE in reduce
      when(valuationService.calculateTotalValue(eq(p), eq(USD), any()))
          .thenReturn(Money.of(100, USD));

      boolean result = snapshotService.snapshotForUser(USER_ID);

      assertThat(result).isTrue();
      verify(snapshotRepository).save(argThat(s -> s.userId().equals(USER_ID) && s.hasStaleData()));
    }

    @Test
    @DisplayName("snapshotForUser: handles empty symbols list")
    void handlesEmptySymbols() {
      Portfolio p = mock(Portfolio.class);
      when(snapshotRepository.existsForToday(USER_ID)).thenReturn(false);
      when(portfolioRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of(p));
      when(p.getDisplayCurrency()).thenReturn(USD);

      // Fix the reduction crash: mock the return value
      when(valuationService.calculateTotalValue(any(), any(), any()))
          .thenReturn(Money.zero(USD));

      snapshotService.snapshotForUser(USER_ID);

      verify(marketDataService, never()).getBatchQuotes(any());
      verify(snapshotRepository).save(any());
    }
  }
}