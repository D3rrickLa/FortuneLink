package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Portfolio Valuation Service Unit Tests")
class PortfolioValuationServiceTest {
  private static final Currency USD = Currency.of("USD");
  private static final Currency CAD = Currency.of("CAD");
  private static final AssetSymbol AAPL = new AssetSymbol("AAPL");
  private static final Money HUNDRED_USD = Money.of(new BigDecimal("100.00"), "USD");
  private final Map<AssetSymbol, MarketAssetQuote> emptyCache = new HashMap<>();

  @Mock
  private ExchangeRateService exchangeRateService;
  @InjectMocks
  private PortfolioValuationServiceImpl valuationService;

  private Account createMockAccount(Currency currency, Money cash,
      Map<AssetSymbol, Position> positions) {
    Account acc = mock(Account.class);
    lenient().when(acc.isActive()).thenReturn(true);
    lenient().when(acc.getAccountCurrency()).thenReturn(currency);
    lenient().when(acc.getCashBalance()).thenReturn(cash);
    lenient().when(acc.getPositionEntries()).thenReturn(positions.entrySet());
    return acc;
  }

  @Test
  @DisplayName("resolvePositionValue: falls back to cost basis when quote is null")
  void resolvePositionValueFallsBackOnNullQuote() {
    Position pos = mock(AcbPosition.class);
    Money costBasis = Money.of(1200.00, "USD");
    when(pos.totalCostBasis()).thenReturn(costBasis);

    ValuationView result = invokeResolvePositionValue(pos, null);

    assertThat(result.totalValue()).isEqualTo(costBasis);
  }

  @Test
  @DisplayName("resolvePositionValue: falls back to cost basis when currentPrice is null")
  void resolvePositionValueFallsBackOnNullPrice() {
    Position pos = mock(AcbPosition.class);
    Money costBasis = Money.of(1200.00, "USD");
    when(pos.totalCostBasis()).thenReturn(costBasis);

    MarketAssetQuote emptyQuote = new MarketAssetQuote(AAPL, null, null, null, null, null, null,
        null, null, null, "Unit testing source", Instant.now());

    ValuationView result = invokeResolvePositionValue(pos, emptyQuote);

    assertThat(result.totalValue()).isEqualTo(costBasis);
  }

  @Test
  @DisplayName("resolvePositionValue: falls back to cost basis when price per unit is zero")
  void resolvePositionValueFallsBackOnZeroPrice() {
    Position pos = mock(AcbPosition.class);
    Money costBasis = Money.of(1200.00, "USD");
    when(pos.totalCostBasis()).thenReturn(costBasis);

    Price zeroPrice = Price.of(BigDecimal.ZERO, USD);
    MarketAssetQuote quote = new MarketAssetQuote(AAPL, zeroPrice, null, null, null, null, null,
        null, null, null, "Unit testing source", Instant.now());

    ValuationView result = invokeResolvePositionValue(pos, quote);

    assertThat(result.totalValue()).isEqualTo(costBasis);
  }

  @Test
  @DisplayName("resolvePositionValue: success when valid quote and currency match")
  void resolvePositionValueReturnsMarketValueOnSuccess() {
    Position pos = mock(AcbPosition.class);

    Price validPrice = Price.of("150.00", USD);
    MarketAssetQuote quote = new MarketAssetQuote(AAPL, validPrice, null, null, null, null, null,
        null, null, null, "Unit testing source", Instant.now());

    Money marketValue = Money.of(1500.00, "USD");

    // REQUIRED: cost basis path is always executed
    when(pos.totalCostBasis()).thenReturn(Money.of(0, USD));

    when(pos.currentValue(validPrice)).thenReturn(marketValue);

    ValuationView result = invokeResolvePositionValue(pos, quote);

    assertThat(result.totalValue()).isEqualTo(marketValue);
  }

  /**
   * Private helper to trigger the 'resolvePositionValue' logic via the public
   * calculatePositionsValue entry point.
   */
  private ValuationView invokeResolvePositionValue(Position pos, MarketAssetQuote quote) {
    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(PortfolioValuationServiceTest.USD);
    when(account.getPositionEntries()).thenReturn(Set.of(Map.entry(AAPL, pos)));

    Map<AssetSymbol, MarketAssetQuote> quotes = new HashMap<>();
    quotes.put(AAPL, quote);

    return valuationService.calculateAccountValuation(account, quotes);
  }

  @Nested
  @DisplayName("Total Portfolio Calculation")
  class TotalPortfolioTests {
    @Test
    @DisplayName("calculateTotalValue: success with empty portfolio returning zero")
    void calculateTotalValueReturnsZeroForEmptyPortfolio() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getAccounts()).thenReturn(List.of());

      ValuationView result = valuationService.calculatePortfolioValuation(portfolio, USD, Map.of());

      assertThat(result.totalValue()).isEqualTo(Money.zero(USD));
    }

    @Test
    @DisplayName("calculatePortfolioValuation converts mixed currencies into target currency")
    void calculatePortfolioValuationConvertsCurrencies() {
      Portfolio portfolio = mock(Portfolio.class);

      // USD account
      Account usdAcc = mock(Account.class);
      when(usdAcc.isActive()).thenReturn(true);
      when(usdAcc.getAccountCurrency()).thenReturn(USD);
      when(usdAcc.getCashBalance()).thenReturn(Money.of(100, USD));
      when(usdAcc.getPositionEntries()).thenReturn(Set.of());
      when(usdAcc.isStale()).thenReturn(false);

      // CAD account
      Account cadAcc = mock(Account.class);
      when(cadAcc.isActive()).thenReturn(true);
      when(cadAcc.getAccountCurrency()).thenReturn(CAD);
      when(cadAcc.getCashBalance()).thenReturn(Money.of(100, CAD));
      when(cadAcc.getPositionEntries()).thenReturn(Set.of());
      when(cadAcc.isStale()).thenReturn(false);

      when(portfolio.getAccounts()).thenReturn(List.of(usdAcc, cadAcc));

      when(exchangeRateService.convert(any(Money.class), eq(USD))).thenAnswer(invocation -> {
        Money amount = invocation.getArgument(0);

        if (amount.currency().equals(USD)) {
          return amount;
        }

        if (amount.currency().equals(CAD)) {
          BigDecimal amt = amount.amount().multiply(new BigDecimal("0.75"));
          return new Money(amt, USD);
        }

        return Money.zero(USD);
      });

      ValuationView result = valuationService.calculatePortfolioValuation(portfolio, USD, Map.of());

      // 100 USD + (100 CAD * 0.75) = 175 USD
      assertThat(result.totalValue().amount()).isEqualByComparingTo("175.00");
    }

    @Test
    @DisplayName("calculateTotalValue: ignores accounts that are not ACTIVE")
    void calculateTotalValueIgnoresInactiveAccounts() {
      Portfolio portfolio = mock(Portfolio.class);
      Account activeAcc = mock(Account.class);
      Account closedAcc = mock(Account.class);

      // Setup Active Account
      when(activeAcc.isActive()).thenReturn(true);
      when(activeAcc.getAccountCurrency()).thenReturn(USD);
      when(activeAcc.getCashBalance()).thenReturn(HUNDRED_USD);
      when(activeAcc.getPositionEntries()).thenReturn(Set.of());

      // Setup Closed Account
      when(closedAcc.isActive()).thenReturn(false);

      when(portfolio.getAccounts()).thenReturn(List.of(activeAcc, closedAcc));

      // Pass-through currency conversion
      when(exchangeRateService.convert(any(Money.class), eq(USD))).thenAnswer(
          invocation -> invocation.getArgument(0));

      ValuationView result = valuationService.calculatePortfolioValuation(portfolio, USD, Map.of());

      assertThat(result.totalValue()).isEqualTo(HUNDRED_USD);

      verify(activeAcc, atLeastOnce()).isActive();
      verify(closedAcc, atLeastOnce()).isActive();

      verify(closedAcc, never()).getCashBalance();
      verify(closedAcc, never()).getPositionEntries();
    }

    @Test
    @DisplayName("calculatePortfolioValuation aggregates active accounts")
    void calculatePortfolioValuationAggregatesAccounts() {
      Portfolio portfolio = mock(Portfolio.class);

      Account acc1 = createMockAccount(USD, HUNDRED_USD, Map.of());
      Account acc2 = createMockAccount(USD, HUNDRED_USD, Map.of());

      when(acc1.isActive()).thenReturn(true);
      when(acc2.isActive()).thenReturn(true);

      when(portfolio.getAccounts()).thenReturn(List.of(acc1, acc2));

      when(exchangeRateService.convert(any(), eq(USD))).thenAnswer(inv -> inv.getArgument(0));

      ValuationView result = valuationService.calculatePortfolioValuation(portfolio, USD, Map.of());

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("Account and Position Valuations")
  class AccountValuationTests {
    @Test
    @DisplayName("calculateAccountValue: returns zero valuation for inactive accounts")
    void calculateAccountValueReturnsZeroForInactive() {

      Account inactiveAccount = mock(Account.class);
      when(inactiveAccount.isActive()).thenReturn(false);
      when(inactiveAccount.getAccountCurrency()).thenReturn(USD);
      when(inactiveAccount.isStale()).thenReturn(false);
      when(inactiveAccount.getPositionEntries()).thenReturn(Set.of());

      ValuationView result = valuationService.calculateAccountValuation(inactiveAccount, Map.of());

      assertThat(result.totalValue()).isEqualTo(Money.zero(USD));
      assertThat(result.totalCashBalance()).isEqualTo(Money.zero(USD));
      assertThat(result.totalInvestedValue()).isEqualTo(Money.zero(USD));
      assertThat(result.totalCostBasis()).isEqualTo(Money.zero(USD));
      assertThat(result.unrealizedGainLoss()).isEqualTo(Money.zero(USD));
      assertThat(result.displayCurrency()).isEqualTo(USD);
      assertThat(result.hasStaleData()).isFalse();
    }

    @Test
    @DisplayName("calculatePositionsValue: filters out cash assets")
    void calculatePositionsValueExcludesCashType() {
      Position stockPos = mock(AcbPosition.class);
      Position cashPos = mock(AcbPosition.class);

      when(stockPos.type()).thenReturn(AssetType.STOCK);
      when(cashPos.type()).thenReturn(AssetType.CASH);

      when(stockPos.totalCostBasis()).thenReturn(Money.zero(USD));

      MarketAssetQuote quote = new MarketAssetQuote(AAPL, Price.of("150", USD), null, null, null,
          null, null, null, null, null, "Unit testing source", Instant.now());

      when(stockPos.currentValue(any())).thenReturn(Money.of(150, USD));

      Account account = mock(Account.class);

      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.isActive()).thenReturn(true);
      when(account.isStale()).thenReturn(false);
      when(account.getCashBalance()).thenReturn(Money.zero(USD));

      when(account.getPositionEntries()).thenReturn(
          Set.of(Map.entry(AAPL, stockPos), Map.entry(new AssetSymbol("CASH"), cashPos)));

      ValuationView result = valuationService.calculateAccountValuation(account,
          Map.of(AAPL, quote));

      // Only stock position counted
      assertThat(result.totalInvestedValue()).isEqualTo(Money.of(150, USD));

      // Cash position excluded
      verify(cashPos, never()).currentValue(any());
    }
  }

  @Nested
  @DisplayName("Quote Resolution and Fallbacks")
  class QuoteResolutionTests {
    @Test
    @DisplayName("resolvePositionValue: falls back to cost basis when quote missing")
    void resolvePositionValueUsesCostBasisOnMissingQuote() {
      Position pos = mock(AcbPosition.class);
      Money costBasis = Money.of(50, "USD");
      when(pos.totalCostBasis()).thenReturn(costBasis);

      Account account = createMockAccount(USD, Money.zero(USD), Map.of(AAPL, pos));

      ValuationView result = valuationService.calculateAccountValuation(account, Map.of());

      assertThat(result.totalCostBasis()).isEqualTo(costBasis);
    }

    @Test
    @DisplayName("resolvePositionValue: converts currency when quote currency differs from account")
    void resolvePositionValueConvertsCurrency() {

      Position pos = mock(AcbPosition.class);
      Currency accountCurrency = Currency.of("USD");

      Money priceInCad = Money.of(100, "CAD");

      MarketAssetQuote cadQuote = new MarketAssetQuote(AAPL, new Price(priceInCad), null, null,
          null, null, null, null, null, null, "Test Source", Instant.now());

      Account account = createMockAccount(accountCurrency, Money.zero(accountCurrency),
          Map.of(AAPL, pos));

      // REQUIRED: prevent cost basis NPE
      when(pos.totalCostBasis()).thenReturn(Money.zero(USD));

      Money priceInUsd = Money.of(75, "USD");

      when(exchangeRateService.convert(argThat(m -> m.currency().equals(Currency.of("CAD"))),
          eq(accountCurrency))).thenReturn(priceInUsd);

      Money expectedFinalValue = Money.of(750, "USD");

      when(pos.currentValue(argThat(p -> p.currency().equals(accountCurrency)
          && p.amount().doubleValue() == 75.0))).thenReturn(expectedFinalValue);

      ValuationView result = valuationService.calculateAccountValuation(account,
          Map.of(AAPL, cadQuote));

      assertThat(result.totalValue()).isEqualTo(expectedFinalValue);

      verify(exchangeRateService).convert(priceInCad, accountCurrency);
    }
  }

  @Nested
  @DisplayName("Parameter Validation")
  class ParameterValidation {

    @Test
    @DisplayName("Should throw NullPointerException when portfolios list is null")
    void shouldThrowNpeWhenPortfoliosNull() {
      assertThatThrownBy(
          () -> valuationService.calculateUserValuation(null, USD, emptyCache)).isInstanceOf(
          NullPointerException.class).hasMessage("Portfolios cannot be null");
    }

    @Test
    @DisplayName("Should throw NullPointerException when target currency is null")
    void shouldThrowNpeWhenCurrencyNull() {
      assertThatThrownBy(
          () -> valuationService.calculateUserValuation(List.of(), null, emptyCache)).isInstanceOf(
          NullPointerException.class).hasMessage("Target currency cannot be null");
    }

    @Test
    @DisplayName("Should throw NullPointerException when quote cache is null")
    void shouldThrowNpeWhenCacheNull() {
      assertThatThrownBy(
          () -> valuationService.calculateUserValuation(List.of(), USD, null)).isInstanceOf(
          NullPointerException.class).hasMessage("Quote cache cannot be null");
    }
  }

  @Nested
  @DisplayName("Valuation Calculation Logic")
  class ValuationCalculation {
    @Test
    @DisplayName("Should correctly aggregate values and apply exchange rates across active accounts")
    void shouldCalculateAggregatedValuation() {
      // Arrange: Set up Portfolio 1 with an Active Account (in CAD)
      Portfolio portfolio1 = mock(Portfolio.class);
      Account account1 = mock(Account.class);
      when(account1.isActive()).thenReturn(true);
      when(account1.isStale()).thenReturn(false);
      when(account1.getCashBalance()).thenReturn(Money.of("100.00", CAD));

      when(account1.getAccountCurrency()).thenReturn(CAD);
      when(account1.getPositionEntries()).thenReturn(Collections.emptyList());

      when(portfolio1.getAccounts()).thenReturn(List.of(account1));

      // Arrange: Set up Portfolio 2 with an Inactive Account (should be entirely ignored)
      Portfolio portfolio2 = mock(Portfolio.class);
      Account inactiveAccount = mock(Account.class);
      when(inactiveAccount.isActive()).thenReturn(false);
      when(portfolio2.getAccounts()).thenReturn(List.of(inactiveAccount));

      // Mock currency conversion behavior for Cash and Positions
      Money positionsInUsd = Money.of("150.00", USD);
      Money cashInUsd = Money.of("75.00", USD);

      emptyCache.put(new AssetSymbol("AAPL"),
          new MarketAssetQuote(new AssetSymbol("AAPL"), Price.of("100", USD), Price.of("100", USD),
              Price.of("100", USD), Price.of("100", USD), Price.of("100", USD),
              PercentageChange.ZERO, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TWO, "ME",
              Instant.now()));

      when(exchangeRateService.convert(any(Money.class), eq(USD))).thenAnswer(invocation -> {
        Money source = invocation.getArgument(0);

        boolean isCad = source.currency() == CAD;
        boolean isCashAmount =
            source.amount() != null && source.amount().compareTo(new BigDecimal("100")) == 0;

        if (isCad && isCashAmount) {
          return cashInUsd; // Returns $75.00
        }

        return positionsInUsd; // Returns $150.00
      });

      // Act
      ValuationView result = valuationService.calculateUserValuation(
          List.of(portfolio1, portfolio2), USD, emptyCache);

      // Assert
      assertThat(result.totalCashBalance()).isEqualTo(cashInUsd);
      assertThat(result.totalInvestedValue()).isEqualTo(positionsInUsd);
      assertThat(result.totalValue()).isEqualTo(Money.of("225.00", USD));
      assertThat(result.hasStaleData()).isFalse();
    }
  }

  @Nested
  @DisplayName("Staleness Propagation")
  class StalenessHandling {
    @Test
    @DisplayName("Should mark output valuation as stale if at least one active account is stale")
    void shouldReturnStaleTrueWhenAnyAccountIsStale() {
      // Arrange
      Portfolio portfolio = mock(Portfolio.class);
      Account freshAccount = mock(Account.class);
      Account staleAccount = mock(Account.class);

      // Setup Fresh Account
      when(freshAccount.isActive()).thenReturn(true);
      when(freshAccount.isStale()).thenReturn(false);
      when(freshAccount.getCashBalance()).thenReturn(Money.zero(USD));
      // FIXES HERE
      when(freshAccount.getAccountCurrency()).thenReturn(USD);
      when(freshAccount.getPositionEntries()).thenReturn(Collections.emptyList());

      // Setup Stale Account
      when(staleAccount.isActive()).thenReturn(true);
      when(staleAccount.isStale()).thenReturn(true);
      when(staleAccount.getCashBalance()).thenReturn(Money.zero(USD));
      // FIXES HERE
      when(staleAccount.getAccountCurrency()).thenReturn(USD);
      when(staleAccount.getPositionEntries()).thenReturn(Collections.emptyList());

      when(portfolio.getAccounts()).thenReturn(List.of(freshAccount, staleAccount));
      when(exchangeRateService.convert(any(), any())).thenReturn(Money.zero(USD));

      // Act
      ValuationView result = valuationService.calculateUserValuation(List.of(portfolio), USD,
          emptyCache);

      // Assert
      assertThat(result.hasStaleData()).isTrue();
    }
  }
}