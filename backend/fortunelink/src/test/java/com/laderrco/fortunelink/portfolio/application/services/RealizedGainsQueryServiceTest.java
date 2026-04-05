package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.queries.GetRealizedGainsQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.RealizedGainsQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainsSummaryView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Realized Gains Query Service Unit Tests")
class RealizedGainsQueryServiceTest {

  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private final int TAX_YEAR = 2023;
  private final Currency USD = Currency.USD;

  @Mock
  private RealizedGainsQueryRepository repository;
  @Mock
  private PortfolioLoader portfolioLoader;
  @InjectMocks
  private RealizedGainsQueryService service;

  private GetRealizedGainsQuery createQuery(Integer year, AssetSymbol symbol) {
    return new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, year, symbol);
  }

  private RealizedGainRecord createRecord(boolean isGain, double amount) {
    RealizedGainRecord record = mock(RealizedGainRecord.class);
    Money value = new Money(BigDecimal.valueOf(amount), USD);

    lenient().when(record.realizedGainLoss()).thenReturn(value);
    lenient().when(record.symbol()).thenReturn(SYMBOL);
    lenient().when(record.isGain()).thenReturn(isGain);
    lenient().when(record.isLoss()).thenReturn(!isGain);
    lenient().when(record.occurredAt()).thenReturn(Instant.now());
    lenient().when(record.costBasisSold()).thenReturn(Money.zero(USD));

    return record;
  }

  @Nested
  @DisplayName("Branching: fetchRecords logic")
  class FetchRecordsTests {
    @Test
    @DisplayName("getRealizedGains: uses Year and Symbol filter when both provided")
    void usesYearAndSymbolFilter() {
      RealizedGainRecord mockRecord = createRecord(true, 100);
      List<RealizedGainRecord> records = List.of(mockRecord);

      GetRealizedGainsQuery query = createQuery(TAX_YEAR, SYMBOL);

      // repository when() call doesn't trigger other when() calls
      when(repository.findByAccountIdAndYearAndSymbol(eq(ACCOUNT_ID), eq(TAX_YEAR),
          eq(SYMBOL))).thenReturn(records);

      service.getRealizedGains(query);

      verify(repository).findByAccountIdAndYearAndSymbol(ACCOUNT_ID, TAX_YEAR, SYMBOL);
    }

    @Test
    @DisplayName("getRealizedGains: uses Year only filter")
    void usesYearOnlyFilter() {
      GetRealizedGainsQuery query = createQuery(TAX_YEAR, null);
      when(repository.findByAccountIdAndYear(ACCOUNT_ID, TAX_YEAR)).thenReturn(List.of());
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      verify(repository).findByAccountIdAndYear(ACCOUNT_ID, TAX_YEAR);
    }

    @Test
    @DisplayName("getRealizedGains: uses Symbol only filter")
    void usesSymbolOnlyFilter() {
      GetRealizedGainsQuery query = createQuery(null, SYMBOL);
      when(repository.findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL)).thenReturn(List.of());
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      verify(repository).findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL);
    }

    @Test
    @DisplayName("getRealizedGains: uses base AccountId filter when no specific params")
    void usesBaseAccountIdFilter() {
      GetRealizedGainsQuery query = createQuery(null, null);
      when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(List.of());
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      verify(repository).findByAccountId(ACCOUNT_ID);
    }

    @Test
    @DisplayName("buildSummary: processes a negative realized gain (a loss)")
    void buildSummaryProcessesLoss() {
      RealizedGainRecord lossRecord = mock(RealizedGainRecord.class);
      // A loss of $50.00
      Money negativeMoney = new Money(new BigDecimal("-50.00"), USD);

      // These must return these values for the 'else if' to trigger
      // when(lossRecord.isGain()).thenReturn(false); // because it's not positive
      // when(lossRecord.isLoss()).thenReturn(true); // because it's negative
      when(lossRecord.realizedGainLoss()).thenReturn(negativeMoney);

      // Metadata needed for the View creation
      when(lossRecord.symbol()).thenReturn(SYMBOL);
      when(lossRecord.costBasisSold()).thenReturn(Money.zero(USD));
      when(lossRecord.occurredAt()).thenReturn(Instant.now());
      when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(lossRecord));

      RealizedGainsSummaryView summary = service.getRealizedGains(createQuery(null, null));

      assertThat(summary.totalLosses()).isEqualTo(new Money(new BigDecimal("50.00"), USD));
    }

    @Test
    @DisplayName("getRealizedGains: correctly handles a breakeven (zero) transaction")
    void handlesBreakevenTransaction() {
      // Create a record with exactly 0.00
      RealizedGainRecord breakEvenRecord = mock(RealizedGainRecord.class);
      Money zeroMoney = Money.zero(USD);

      lenient().when(breakEvenRecord.isGain()).thenReturn(false); // isPositive() is false
      lenient().when(breakEvenRecord.isLoss()).thenReturn(false); // isNegative() is false
      when(breakEvenRecord.realizedGainLoss()).thenReturn(zeroMoney);

      // Metadata for the View mapping
      when(breakEvenRecord.symbol()).thenReturn(SYMBOL);
      when(breakEvenRecord.costBasisSold()).thenReturn(zeroMoney);
      when(breakEvenRecord.occurredAt()).thenReturn(Instant.now());

      when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(breakEvenRecord));

      RealizedGainsSummaryView summary = service.getRealizedGains(createQuery(null, null));

      assertThat(summary.totalGains()).isEqualTo(zeroMoney);
      assertThat(summary.totalLosses()).isEqualTo(zeroMoney);
      assertThat(summary.netGainLoss()).isEqualTo(zeroMoney);

      // The transaction should still appear in the list of views
      assertThat(summary.items()).hasSize(1);
      assertThat(summary.items().get(0).realizedGainLoss()).isEqualTo(zeroMoney);
    }
  }

  @Nested
  @DisplayName("Currency Resolution Logic")
  class CurrencyResolutionTests {
    @Test
    @DisplayName("resolveCurrency: derives from first record when list is not empty")
    void derivesFromFirstRecord() {
      GetRealizedGainsQuery query = createQuery(null, null);
      RealizedGainRecord record = createRecord(true, 50.0);
      when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(record));

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      assertThat(summary.currency()).isEqualTo(USD);
      verify(repository, never()).findAccountCurrencyCode(any());
    }

    @Test
    @DisplayName("resolveCurrency: falls back to repository lookup when records are empty")
    void fallsBackToRepositoryLookup() {
      GetRealizedGainsQuery query = createQuery(null, null);
      when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(List.of());
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      assertThat(summary.currency()).isEqualTo(USD);
      verify(repository).findAccountCurrencyCode(ACCOUNT_ID);
    }

    @Test
    @DisplayName("resolveCurrency: uses CAD as ultimate safe fallback")
    void ultimateSafeFallback() {
      GetRealizedGainsQuery query = createQuery(null, null);
      when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(List.of());
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.empty());

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      assertThat(summary.currency()).isEqualTo(Currency.CAD);
    }
  }

  @Nested
  @DisplayName("Summary Calculation (Math)")
  class CalculationTests {
    @Test
    @DisplayName("buildSummary: correctly totals gains, losses, and net values")
    void correctlyCalculatesSummary() {
      GetRealizedGainsQuery query = createQuery(TAX_YEAR, null);
      RealizedGainRecord gain1 = createRecord(true, 150.00);
      RealizedGainRecord loss1 = createRecord(false, 50.00); // 50 loss

      when(repository.findByAccountIdAndYear(ACCOUNT_ID, TAX_YEAR)).thenReturn(
          List.of(gain1, loss1));

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      // 150 (Gain) - 50 (Loss) = 100 Net
      assertThat(summary.totalGains()).isEqualTo(new Money(new BigDecimal("150.0"), USD));
      assertThat(summary.totalLosses()).isEqualTo(new Money(new BigDecimal("50.0"), USD));
      assertThat(summary.netGainLoss()).isEqualTo(new Money(new BigDecimal("100.0"), USD));
      assertThat(summary.items()).hasSize(2);
      assertThat(summary.taxYear()).isEqualTo(TAX_YEAR);
    }
  }

  @Nested
  @DisplayName("Security and Validation")
  class SecurityTests {
    @Test
    @DisplayName("getRealizedGains: validates ownership before fetching data")
    void validatesOwnershipBeforeFetch() {
      GetRealizedGainsQuery query = createQuery(TAX_YEAR, SYMBOL);
      when(repository.findByAccountIdAndYearAndSymbol(any(), anyInt(), any())).thenReturn(
          List.of());
      when(repository.findAccountCurrencyCode(any())).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      verify(portfolioLoader).validatePortfolioAndAccountOwnership(eq(PORTFOLIO_ID), eq(USER_ID),
          eq(ACCOUNT_ID));

      verify(repository).findByAccountIdAndYearAndSymbol(any(AccountId.class), anyInt(),
          any(AssetSymbol.class));
    }

    @Test
    @DisplayName("getRealizedGains: throws exception when query is null")
    void throwsOnNullQuery() {
      assertThatThrownBy(() -> service.getRealizedGains(null)).isInstanceOf(
          NullPointerException.class).hasMessageContaining("cannot be null");
    }
  }
}