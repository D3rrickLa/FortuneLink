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
import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.GainsAggregation;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

    return new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, year, symbol, 0, 20);
  }

  private RealizedGainRecord createRecord(boolean isGain, double amount) {
    RealizedGainRecord record = mock(RealizedGainRecord.class);
    Money value = new Money(BigDecimal.valueOf(amount), USD);

    lenient().when(record.realizedGainLoss()).thenReturn(value);
    lenient().when(record.symbol()).thenReturn(SYMBOL);
    lenient().when(record.isGain()).thenReturn(isGain);
    lenient().when(record.occurredAt()).thenReturn(Instant.now());
    lenient().when(record.costBasisSold()).thenReturn(Money.zero(USD));

    return record;
  }

  private void mockTotals(double sumGains, double sumLosses) {
    lenient().when(repository.calculateTotals(any(), any(), any())).thenReturn(
        new GainsAggregation(BigDecimal.valueOf(sumGains), BigDecimal.valueOf(sumLosses)));
  }

  @Nested
  @DisplayName("Branching: fetchRecords logic")
  class FetchRecordsTests {
    @Test
    @DisplayName("getRealizedGains: uses Year and Symbol filter when both provided")
    void usesYearAndSymbolFilter() {
      GetRealizedGainsQuery query = createQuery(TAX_YEAR, SYMBOL);
      Page<RealizedGainRecord> page = new PageImpl<>(List.of(createRecord(true, 100)));

      when(repository.findByAccountIdAndYearAndSymbol(eq(ACCOUNT_ID), eq(TAX_YEAR), eq(SYMBOL),
          any(Pageable.class))).thenReturn(page);
      mockTotals(100.0, 0.0);
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      verify(repository).findByAccountIdAndYearAndSymbol(eq(ACCOUNT_ID), eq(TAX_YEAR), eq(SYMBOL),
          any(Pageable.class));
    }

    @Test
    @DisplayName("getRealizedGains: uses Year only filter")
    void usesYearOnlyFilter() {
      GetRealizedGainsQuery query = createQuery(TAX_YEAR, null);
      when(repository.findByAccountIdAndYear(eq(ACCOUNT_ID), eq(TAX_YEAR),
          any(Pageable.class))).thenReturn(Page.empty());
      mockTotals(0.0, 0.0);
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      verify(repository).findByAccountIdAndYear(eq(ACCOUNT_ID), eq(TAX_YEAR), any(Pageable.class));
    }

    @Test
    @DisplayName("getRealizedGains: uses Symbol only filter")
    void usesSymbolOnlyFilter() {
      GetRealizedGainsQuery query = createQuery(null, SYMBOL);
      Page<RealizedGainRecord> page = new PageImpl<>(List.of(createRecord(true, 50.0)));

      when(repository.findByAccountIdAndSymbol(eq(ACCOUNT_ID), eq(SYMBOL),
          any(Pageable.class))).thenReturn(page);
      mockTotals(50.0, 0.0);
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      verify(repository).findByAccountIdAndSymbol(eq(ACCOUNT_ID), eq(SYMBOL), any(Pageable.class));

      verify(repository, never()).findByAccountIdAndYear(any(), anyInt(), any());
      verify(repository, never()).findByAccountIdAndYearAndSymbol(any(), anyInt(), any(), any());

      assertThat(summary.items()).hasSize(1);
    }

    @Test
    @DisplayName("getRealizedGains: passes correct pagination parameters to repository")
    void passesPaginationToRepository() {
      int requestedPage = 5;
      int requestedSize = 10;
      GetRealizedGainsQuery query = new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          null, null, requestedPage, requestedSize);

      when(repository.findByAccountId(eq(ACCOUNT_ID), any(Pageable.class))).thenReturn(
          Page.empty());
      mockTotals(0.0, 0.0);
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(repository).findByAccountId(eq(ACCOUNT_ID), captor.capture());

      Pageable sentPageable = captor.getValue();
      assertThat(sentPageable.getPageNumber()).isEqualTo(requestedPage);
      assertThat(sentPageable.getPageSize()).isEqualTo(requestedSize);
    }

    @Test
    @DisplayName("getRealizedGains: should return ZERO totals when repository returns null aggregation")
    void handlesNullAggregationFromRepository() {
      GetRealizedGainsQuery query = new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          2023, null, 0, 10);
      GainsAggregation nullAggregation = new GainsAggregation(null, null);

      when(repository.findByAccountIdAndYear(any(), anyInt(), any())).thenReturn(
          new PageImpl<>(List.of()));
      when(repository.calculateTotals(eq(ACCOUNT_ID), eq(2023), any())).thenReturn(nullAggregation);
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      RealizedGainsSummaryView result = service.getRealizedGains(query);

      assertThat(result.totalGains().amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.totalLosses().amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.netGainLoss().amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.currency()).isEqualTo(Currency.USD);
    }
  }

  @Nested
  @DisplayName("Currency Resolution Logic")
  class CurrencyResolutionTests {
    @Test
    @DisplayName("resolveCurrency: always uses repository lookup (Account Source of Truth)")
    void usesRepositoryLookup() {
      GetRealizedGainsQuery query = createQuery(null, null);
      when(repository.findByAccountId(eq(ACCOUNT_ID), any(Pageable.class))).thenReturn(
          Page.empty());
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));
      mockTotals(0.0, 0.0);

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      assertThat(summary.currency()).isEqualTo(USD);
      verify(repository).findAccountCurrencyCode(ACCOUNT_ID);
    }

    @Test
    @DisplayName("resolveCurrency: uses CAD as ultimate safe fallback")
    void ultimateSafeFallback() {
      GetRealizedGainsQuery query = createQuery(null, null);
      when(repository.findByAccountId(eq(ACCOUNT_ID), any(Pageable.class))).thenReturn(
          Page.empty());
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.empty());
      mockTotals(0.0, 0.0);

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      assertThat(summary.currency()).isEqualTo(Currency.CAD);
    }
  }

  @Nested
  @DisplayName("Summary Calculation (Math)")
  class CalculationTests {
    @Test
    @DisplayName("buildSummary: uses DB aggregation for totals, not the page content")
    void correctlyCalculatesSummary() {
      GetRealizedGainsQuery query = createQuery(TAX_YEAR, null);
      // Page only contains ONE record
      RealizedGainRecord gain1 = createRecord(true, 150.00);
      Page<RealizedGainRecord> page = new PageImpl<>(List.of(gain1));

      when(repository.findByAccountIdAndYear(eq(ACCOUNT_ID), eq(TAX_YEAR),
          any(Pageable.class))).thenReturn(page);

      mockTotals(150.0, 50.0);
      when(repository.findAccountCurrencyCode(ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      RealizedGainsSummaryView summary = service.getRealizedGains(query);

      assertThat(summary.totalGains()).isEqualTo(new Money(new BigDecimal("150.0"), USD));
      assertThat(summary.totalLosses()).isEqualTo(new Money(new BigDecimal("50.0"), USD));
      assertThat(summary.netGainLoss()).isEqualTo(new Money(new BigDecimal("100.0"), USD));

      assertThat(summary.items()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Security and Validation")
  class SecurityTests {
    @Test
    @DisplayName("getRealizedGains: validates ownership before fetching data")
    void validatesOwnershipBeforeFetch() {
      GetRealizedGainsQuery query = createQuery(TAX_YEAR, SYMBOL);
      when(repository.findByAccountIdAndYearAndSymbol(any(), anyInt(), any(), any())).thenReturn(
          Page.empty());
      mockTotals(0.0, 0.0);
      when(repository.findAccountCurrencyCode(any())).thenReturn(Optional.of("USD"));

      service.getRealizedGains(query);

      verify(portfolioLoader).validatePortfolioAndAccountOwnership(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID);
    }

    @Test
    @DisplayName("getRealizedGains: throws exception when query is null")
    void throwsOnNullQuery() {
      assertThatThrownBy(() -> service.getRealizedGains(null)).isInstanceOf(
          NullPointerException.class).hasMessageContaining("cannot be null");
    }
  }
}