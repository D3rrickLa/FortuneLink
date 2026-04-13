package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
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
@DisplayName("User Snapshot Worker Unit Tests")
class UserSnapshotWorkerTest {

  private final UserId USER_ID = UserId.random();
  private final Currency USD = Currency.of("USD");

  @Mock
  private MarketDataService marketDataService;

  @Mock
  private PortfolioRepository portfolioRepository;

  @Mock
  private NetWorthSnapshotRepository snapshotRepository;

  @Mock
  private PortfolioValuationService valuationService;

  @InjectMocks
  private UserSnapshotWorker snapshotWorker; 

  @Test
  @DisplayName("snapshotForUser: tests quote fetch branching (empty symbols vs symbols present)")
  void snapshotForUserQuoteFetchingBranching() {
    
    Portfolio p = mock(Portfolio.class);
    when(snapshotRepository.existsForToday(USER_ID)).thenReturn(false);
    when(portfolioRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of(p));
    when(p.getDisplayCurrency()).thenReturn(USD);
    when(valuationService.calculateTotalValue(any(), any(), any())).thenReturn(Money.zero(USD));

    
    snapshotWorker.snapshotForUser(USER_ID);
    verify(marketDataService, never()).getBatchQuotes(any());

    
    AssetSymbol apple = new AssetSymbol("AAPL");
    try (MockedStatic<PortfolioAccessUtils> utils = mockStatic(PortfolioAccessUtils.class)) {
      utils.when(() -> PortfolioAccessUtils.extractSymbols(p)).thenReturn(Set.of(apple));

      snapshotWorker.snapshotForUser(USER_ID);

      verify(marketDataService, times(1)).getBatchQuotes(Set.of(apple));
    }
  }

  @Test
  @DisplayName("snapshotForUser: returns false when no work needed")
  void returnsFalseWhenNoWork() {
    
    when(snapshotRepository.existsForToday(USER_ID)).thenReturn(true);
    assertThat(snapshotWorker.snapshotForUser(USER_ID)).isFalse();

    
    when(snapshotRepository.existsForToday(USER_ID)).thenReturn(false);
    when(portfolioRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
    assertThat(snapshotWorker.snapshotForUser(USER_ID)).isFalse();
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

    when(valuationService.calculateTotalValue(eq(p), eq(USD), any()))
        .thenReturn(Money.of(100, USD));

    boolean result = snapshotWorker.snapshotForUser(USER_ID);

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

    when(valuationService.calculateTotalValue(any(), any(), any()))
        .thenReturn(Money.zero(USD));

    snapshotWorker.snapshotForUser(USER_ID);

    verify(marketDataService, never()).getBatchQuotes(any());
    verify(snapshotRepository).save(any());
  }
}