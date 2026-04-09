package com.laderrco.fortunelink.portfolio.application.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.services.AccountHealthService;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionRecalculationExecutorTest {
  private static final PortfolioId P_ID = PortfolioId.newId();
  private static final UserId U_ID = UserId.random();
  private static final AccountId A_ID = AccountId.newId();
  private static final AssetSymbol SYMBOL = new AssetSymbol("AAPL");

  @Mock
  private PortfolioRepository portfolioRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private TransactionRecordingService transactionRecordingService;
  @Mock
  private AccountHealthService accountHealthService;
  @Mock
  private PortfolioLoader portfolioLoader;

  @Mock
  private Portfolio portfolio;
  @Mock
  private Account account;
  @Mock
  private Transaction transaction;
  @Mock
  private TransactionType txType;

  @InjectMocks
  private PositionRecalculationExecutor executor;

  @BeforeEach
  void setUp() {
    when(portfolioLoader.loadUserPortfolio(P_ID, U_ID)).thenReturn(portfolio);
    when(portfolio.getAccount(A_ID)).thenReturn(account);
  }

  @Nested
  @DisplayName("scheduleRecalculation: Surgical Symbol Update")
  class ScheduleRecalculationTests {
    @Test
    @DisplayName("scheduleRecalculation: should replay only non-excluded transactions that affect holdings")
    void scheduleRecalculation_shouldFilterAndReplayCorrectTransactions() {

      when(transaction.isExcluded()).thenReturn(false);
      when(transaction.transactionType()).thenReturn(txType);
      when(txType.affectsHoldings()).thenReturn(true);
      when(transactionRepository.findByAccountIdAndSymbol(A_ID, SYMBOL)).thenReturn(
          List.of(transaction));

      executor.scheduleRecalculation(P_ID, U_ID, A_ID, SYMBOL);

      verify(account).beginReplay();
      verify(transactionRecordingService).replayTransaction(account, transaction);
      verify(account).endReplay();
      verify(portfolio).reportRecalculationSuccess(A_ID);
      verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("scheduleRecalculation: should mark account stale and rethrow when replay fails")
    void scheduleRecalculation_shouldHandleErrors() {

      when(transactionRepository.findByAccountIdAndSymbol(A_ID, SYMBOL)).thenReturn(
          List.of(transaction));
      when(transaction.isExcluded()).thenReturn(false);
      when(transaction.transactionType()).thenReturn(txType);
      when(txType.affectsHoldings()).thenReturn(true);

      doThrow(new RuntimeException("Replay Failed")).when(transactionRecordingService)
          .replayTransaction(any(), any());

      assertThrows(RuntimeException.class,
          () -> executor.scheduleRecalculation(P_ID, U_ID, A_ID, SYMBOL));

      verify(accountHealthService).markStale(A_ID);
      verify(portfolioRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("replayFullAccount: Total Account Reconstruction")
  class ReplayFullAccountTests {

    @Test
    @DisplayName("replayFullAccount: should pass all active transactions to the recording service")
    void replayFullAccount_shouldSucceed() {

      List<Transaction> transactions = List.of(transaction);
      when(transaction.isExcluded()).thenReturn(false);
      when(transactionRepository.findByPortfolioIdAndUserIdAndAccountId(P_ID, U_ID,
          A_ID)).thenReturn(transactions);

      executor.replayFullAccount(P_ID, U_ID, A_ID);

      verify(transactionRecordingService).replayFullTransaction(eq(account), anyList());
      verify(portfolio).reportRecalculationSuccess(A_ID);
      verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("replayFullAccount: should mark account stale when full replay fails")
    void replayFullAccount_shouldHandleErrors() {

      when(transactionRepository.findByPortfolioIdAndUserIdAndAccountId(any(), any(),
          any())).thenReturn(Collections.emptyList());

      doThrow(new RuntimeException("Critical Failure")).when(transactionRecordingService)
          .replayFullTransaction(any(), any());

      assertThrows(RuntimeException.class, () -> executor.replayFullAccount(P_ID, U_ID, A_ID));

      verify(accountHealthService).markStale(A_ID);
      verify(portfolioRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Filtering Logic: Transaction Exclusion and Type Filtering")
  class FilteringTests {

    @Test
    @DisplayName("scheduleRecalculation: should filter out excluded transactions and non-holding types")
    void scheduleRecalculation_shouldOnlyReplayValidHoldingTransactions() {
      Transaction validTx = mock(Transaction.class);
      when(validTx.isExcluded()).thenReturn(false);
      TransactionType holdingType = mock(TransactionType.class);
      when(holdingType.affectsHoldings()).thenReturn(true);
      when(validTx.transactionType()).thenReturn(holdingType);

      Transaction excludedTx = mock(Transaction.class);
      when(excludedTx.isExcluded()).thenReturn(true);

      Transaction nonHoldingTx = mock(Transaction.class);
      when(nonHoldingTx.isExcluded()).thenReturn(false);
      TransactionType cashType = mock(TransactionType.class);
      when(cashType.affectsHoldings()).thenReturn(false);
      when(nonHoldingTx.transactionType()).thenReturn(cashType);

      when(transactionRepository.findByAccountIdAndSymbol(A_ID, SYMBOL)).thenReturn(
          List.of(validTx, excludedTx, nonHoldingTx));

      executor.scheduleRecalculation(P_ID, U_ID, A_ID, SYMBOL);

      verify(transactionRecordingService, times(1)).replayTransaction(any(), eq(validTx));
      verify(transactionRecordingService, never()).replayTransaction(any(), eq(excludedTx));
      verify(transactionRecordingService, never()).replayTransaction(any(), eq(nonHoldingTx));
    }

    @Test
    @DisplayName("replayFullAccount: should filter out excluded transactions but include all types")
    void replayFullAccount_shouldFilterExcludedButIncludeCashEvents() {
      Transaction cashTx = mock(Transaction.class);
      when(cashTx.isExcluded()).thenReturn(false);
      when(cashTx.occurredAt()).thenReturn(Instant.now().minusSeconds(10));

      Transaction holdingTx = mock(Transaction.class);
      when(holdingTx.isExcluded()).thenReturn(false);
      when(holdingTx.occurredAt()).thenReturn(Instant.now());

      Transaction excludedTx = mock(Transaction.class);
      when(excludedTx.isExcluded()).thenReturn(true);

      when(transactionRepository.findByPortfolioIdAndUserIdAndAccountId(P_ID, U_ID,
          A_ID)).thenReturn(List.of(cashTx, holdingTx, excludedTx));

      executor.replayFullAccount(P_ID, U_ID, A_ID);

      ArgumentCaptor<List<Transaction>> listCaptor = ArgumentCaptor.forClass(List.class);
      verify(transactionRecordingService).replayFullTransaction(eq(account), listCaptor.capture());

      List<Transaction> capturedList = listCaptor.getValue();
      assertEquals(2, capturedList.size(), "Should include both cash and holding transactions");
      assertFalse(capturedList.contains(excludedTx), "Should not contain excluded transaction");
    }
  }
}