package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidDateRangeException;
import com.laderrco.fortunelink.portfolio.application.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionForCalculationQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionQueryService Unit Tests")
public class TransactionQueryServiceTest {
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final UserId USER_ID = UserId.random();
  private static final AccountId ACCOUNT_ID = AccountId.newId();
  private static final TransactionId TX_ID = TransactionId.newId();
  private static final Instant START = Instant.parse("2023-01-01T00:00:00Z");
  private static final Instant END = Instant.parse("2023-12-31T23:59:59Z");
  private static final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private TransactionQueryRepository transactionQueryRepository;
  @Mock
  private TransactionViewMapper transactionViewMapper;
  @Mock
  private PortfolioLoader portfolioLoader;
  @InjectMocks
  private TransactionQueryService transactionQueryService;

  private GetTransactionByIdQuery createGetByIdQuery() {
    return new GetTransactionByIdQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, TX_ID);
  }

  private GetTransactionHistoryQuery createHistoryQuery(Instant start, Instant end,
      AssetSymbol symbol) {
    return new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, symbol, start, end, 0,
        10);
  }

  private GetTransactionForCalculationQuery createCalcQuery(Instant start, Instant end) {
    return new GetTransactionForCalculationQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, start, end);
  }

  @Nested
  @DisplayName("getTransaction")
  class GetTransactionTests {
    @Test
    @DisplayName("getTransaction: throws exception when transaction not found")
    void getTransactionThrowsNotFound() {
      GetTransactionByIdQuery query = createGetByIdQuery();
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> transactionQueryService.getTransaction(query)).isInstanceOf(
          TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("getTransaction: returns mapped view when found")
    void getTransactionReturnsView() {
      GetTransactionByIdQuery query = createGetByIdQuery();
      Transaction mockTx = Mockito.mock(Transaction.class);
      TransactionView mockView = Mockito.mock(TransactionView.class);

      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(TX_ID, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID)).thenReturn(Optional.of(mockTx));
      when(transactionViewMapper.toTransactionView(mockTx)).thenReturn(mockView);

      TransactionView result = transactionQueryService.getTransaction(query);

      assertThat(result).isEqualTo(mockView);
    }
  }

  @Nested
  @DisplayName("getTransactionHistory Filtering & Validation")
  class GetTransactionHistoryTests {
    @Test
    @DisplayName("getTransactionHistory: throws exception when both date range and symbol provided")
    void getTransactionHistoryThrowsOnMutualExclusion() {
      GetTransactionHistoryQuery query = createHistoryQuery(START, END, SYMBOL);

      assertThatThrownBy(() -> transactionQueryService.getTransactionHistory(query)).isInstanceOf(
              IllegalArgumentException.class)
          .hasMessageContaining("Cannot filter by both date range and symbol");
    }

    @Test
    @DisplayName("getTransactionHistory: uses date range filter when only dates provided")
    void getTransactionHistoryUsesDateRange() {
      GetTransactionHistoryQuery query = createHistoryQuery(START, END, null);
      Page<Transaction> mockPage = new PageImpl<>(List.of());

      when(
          transactionQueryRepository.findByAccountIdAndDateRange(eq(ACCOUNT_ID), eq(START), eq(END),
              any())).thenReturn(mockPage);

      transactionQueryService.getTransactionHistory(query);

      verify(transactionQueryRepository).findByAccountIdAndDateRange(eq(ACCOUNT_ID), eq(START),
          eq(END), any());
    }

    @Test
    @DisplayName("getTransactionHistory: uses symbol filter when only symbol provided")
    void getTransactionHistoryUsesSymbol() {
      GetTransactionHistoryQuery query = createHistoryQuery(null, null, SYMBOL);
      Page<Transaction> mockPage = new PageImpl<>(List.of());

      when(transactionQueryRepository.findByAccountIdAndSymbol(eq(ACCOUNT_ID), eq(SYMBOL),
          any())).thenReturn(mockPage);

      transactionQueryService.getTransactionHistory(query);

      verify(transactionQueryRepository).findByAccountIdAndSymbol(eq(ACCOUNT_ID), eq(SYMBOL),
          any());
    }

    @Test
    @DisplayName("getTransactionHistory: uses no-filter path when neither dates nor symbol provided")
    void getTransactionHistoryUsesNoFilter() {
      GetTransactionHistoryQuery query = createHistoryQuery(null, null, null);
      Page<Transaction> mockPage = new PageImpl<>(List.of());

      when(transactionQueryRepository.findByAccountId(eq(ACCOUNT_ID), any())).thenReturn(mockPage);

      transactionQueryService.getTransactionHistory(query);

      verify(transactionQueryRepository).findByAccountId(eq(ACCOUNT_ID), any());
    }

    @Nested
    @DisplayName("getTransactionHistory Partial Filters and Defaults")
    class GetTransactionHistoryPartialFilterTests {
      @Test
      @DisplayName("getTransactionHistory: uses symbol filter when start date is provided without end date")
      void getTransactionHistoryUsesSymbolWhenDateRangeIsPartial() {
        // hasDateRange will be false because endDate is null
        GetTransactionHistoryQuery query = createHistoryQuery(START, null, SYMBOL);
        Page<Transaction> mockPage = new PageImpl<>(List.of());

        when(transactionQueryRepository.findByAccountIdAndSymbol(eq(ACCOUNT_ID), eq(SYMBOL),
            any())).thenReturn(mockPage);

        transactionQueryService.getTransactionHistory(query);

        // Verify it skipped findByAccountIdAndDateRange and went to Symbol
        verify(transactionQueryRepository).findByAccountIdAndSymbol(eq(ACCOUNT_ID), eq(SYMBOL),
            any());
      }

      @Test
      @DisplayName("getTransactionHistory: defaults to basic account query when only end date is provided")
      void getTransactionHistoryDefaultsToBasicWhenOnlyEndDateProvided() {
        // hasDateRange = false, hasSymbol = false
        GetTransactionHistoryQuery query = createHistoryQuery(null, END, null);
        Page<Transaction> mockPage = new PageImpl<>(List.of());

        when(transactionQueryRepository.findByAccountId(eq(ACCOUNT_ID), any())).thenReturn(
            mockPage);

        transactionQueryService.getTransactionHistory(query);

        // Verify it hits the final 'else' block
        verify(transactionQueryRepository).findByAccountId(eq(ACCOUNT_ID), any());
      }
    }
  }

  @Nested
  @DisplayName("Internal Calculation & Security")
  class InternalCalculationTests {
    @Test
    @DisplayName("getTransactionsForCalculation: returns list of Transactions, happy path")
    void GetTransactionForCalculationReturnsTransactionList() {
      GetTransactionForCalculationQuery query = createCalcQuery(START, END);

      List<Transaction> transactions = mock();
      when(transactionRepository.findByAccountIdAndDateRange(eq(ACCOUNT_ID), eq(START),
          eq(END))).thenReturn(transactions);

      List<Transaction> results = transactionQueryService.getTransactionsForCalculation(query);
      assertThat(results).isEqualTo(transactions);
    }

    @Test
    @DisplayName("getTransactionsForCalculation: throws exception for invalid date range")
    void getTransactionsForCalculationThrowsOnInvalidDates() {
      // Start is after End
      GetTransactionForCalculationQuery query = createCalcQuery(END, START);

      assertThatThrownBy(
          () -> transactionQueryService.getTransactionsForCalculation(query)).isInstanceOf(
          InvalidDateRangeException.class);
    }

    @Test
    @DisplayName("getTransactionsForCalculation: validates portfolio ownership before fetching")
    void getTransactionsForCalculationValidatesOwnership() {
      GetTransactionForCalculationQuery query = createCalcQuery(START, END);

      // Mocking a failure in ownership validation (e.g., account belongs to different
      // portfolio)
      Mockito.doThrow(new SecurityException("Unauthorized")).when(portfolioLoader)
          .validatePortfolioAndAccountOwnership(PORTFOLIO_ID, USER_ID, ACCOUNT_ID);

      assertThatThrownBy(
          () -> transactionQueryService.getTransactionsForCalculation(query)).isInstanceOf(
          SecurityException.class);
    }

    @Test
    @DisplayName("getTransactionsForCalculation: throws NPE on null query parameters")
    void getTransactionsForCalculationGuardChecks() {
      GetTransactionForCalculationQuery nullQuery = createCalcQuery(null, null);
      // This tests the explicit Objects.requireNonNull checks in your method
      assertThatThrownBy(
          () -> transactionQueryService.getTransactionsForCalculation(nullQuery)).isInstanceOf(
          NullPointerException.class);
    }
  }
}
