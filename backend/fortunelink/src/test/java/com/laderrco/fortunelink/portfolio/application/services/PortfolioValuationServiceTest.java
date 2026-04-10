package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import java.math.BigDecimal;
import java.time.Instant;
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
  private static final Money HUNDRED_CAD = Money.of(new BigDecimal("100.00"), "CAD");
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

    Money result = invokeResolvePositionValue(pos, null, USD);

    assertThat(result).isEqualTo(costBasis);
  }

  @Test
  @DisplayName("resolvePositionValue: falls back to cost basis when currentPrice is null")
  void resolvePositionValueFallsBackOnNullPrice() {
    Position pos = mock(AcbPosition.class);
    Money costBasis = Money.of(1200.00, "USD");
    when(pos.totalCostBasis()).thenReturn(costBasis);

    MarketAssetQuote emptyQuote = new MarketAssetQuote(AAPL, null, null, null, null, null, null,
        null, null, null, "Unit testing source", Instant.now());

    Money result = invokeResolvePositionValue(pos, emptyQuote, USD);

    assertThat(result).isEqualTo(costBasis);
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

    Money result = invokeResolvePositionValue(pos, quote, USD);

    assertThat(result).isEqualTo(costBasis);
  }

  @Test
  @DisplayName("resolvePositionValue: success when valid quote and currency match")
  void resolvePositionValueReturnsMarketValueOnSuccess() {
    Position pos = mock(AcbPosition.class);
    Price validPrice = Price.of("150.00", USD);
    MarketAssetQuote quote = new MarketAssetQuote(AAPL, validPrice, null, null, null, null, null,
        null, null, null, "Unit testing source", Instant.now());
    Money marketValue = Money.of(1500.00, "USD");

    when(pos.currentValue(validPrice)).thenReturn(marketValue);

    Money result = invokeResolvePositionValue(pos, quote, USD);

    assertThat(result).isEqualTo(marketValue);
  }

  /**
   * Private helper to trigger the 'resolvePositionValue' logic via the public
   * calculatePositionsValue entry point.
   */
  private Money invokeResolvePositionValue(Position pos, MarketAssetQuote quote,
      Currency currency) {
    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(currency);
    when(account.getPositionEntries()).thenReturn(Set.of(Map.entry(AAPL, pos)));

    Map<AssetSymbol, MarketAssetQuote> quotes = new HashMap<>();
    quotes.put(AAPL, quote);

    return valuationService.calculatePositionsValue(account, quotes);
  }

  @Nested
  @DisplayName("Total Portfolio Calculation")
  class TotalPortfolioTests {
    @Test
    @DisplayName("calculateTotalValue: success with empty portfolio returning zero")
    void calculateTotalValueReturnsZeroForEmptyPortfolio() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getAccounts()).thenReturn(List.of());

      Money result = valuationService.calculateTotalValue(portfolio, USD, Map.of());

      assertThat(result).isEqualTo(Money.zero(USD));
    }

    @Test
    @DisplayName("calculateTotalValue: aggregates by currency before converting")
    void calculateTotalValueGroupsByCurrency() {
      Portfolio portfolio = mock(Portfolio.class);
      Account usdAcc1 = createMockAccount(USD, HUNDRED_USD, Map.of());
      Account usdAcc2 = createMockAccount(USD, HUNDRED_USD, Map.of());
      Account cadAcc = createMockAccount(CAD, HUNDRED_CAD, Map.of());

      List.of(usdAcc1, usdAcc2, cadAcc)
          .forEach(acc -> when(acc.getState()).thenReturn(AccountLifecycleState.ACTIVE));

      when(valuationService.calculateAccountValue(usdAcc1, Map.of())).thenReturn(HUNDRED_USD);
      when(valuationService.calculateAccountValue(usdAcc2, Map.of())).thenReturn(HUNDRED_USD);
      when(valuationService.calculateAccountValue(cadAcc, Map.of())).thenReturn(HUNDRED_CAD);
      when(portfolio.getAccounts()).thenReturn(List.of(usdAcc1, usdAcc2, cadAcc));

      when(exchangeRateService.convert(argThat(m -> m != null && m.amount().intValue() == 200),
          eq(USD))).thenReturn(Money.of(200, "USD"));
      when(exchangeRateService.convert(argThat(m -> m != null && m.amount().intValue() == 100),
          eq(USD))).thenReturn(Money.of(75, "USD"));

      Money total = valuationService.calculateTotalValue(portfolio, USD, Map.of());

      verify(exchangeRateService, times(2)).convert(any(), eq(USD));
      assertThat(total).isEqualTo(Money.of(275, "USD"));
    }

    @Test
    @DisplayName("calculateTotalValue: ignores accounts that are not ACTIVE")
    void calculateTotalValueIgnoresInactiveAccounts() {

      Portfolio portfolio = mock(Portfolio.class);
      Account activeAcc = createMockAccount(USD, HUNDRED_USD, Map.of());
      Account closedAcc = createMockAccount(USD, HUNDRED_USD, Map.of());

      when(activeAcc.getState()).thenReturn(AccountLifecycleState.ACTIVE);
      when(closedAcc.getState()).thenReturn(AccountLifecycleState.CLOSED);
      when(portfolio.getAccounts()).thenReturn(List.of(activeAcc, closedAcc));

      when(exchangeRateService.convert(any(), eq(USD))).thenReturn(HUNDRED_USD);

      Money total = valuationService.calculateTotalValue(portfolio, USD, Map.of());

      assertThat(total).isEqualTo(HUNDRED_USD);

      verify(exchangeRateService, times(1)).convert(any(), eq(USD));
    }

    @Test
    @DisplayName("calculateTotalValue: skips accounts that return null valuation")
    void calculateTotalValueSkipsNullValuations() {

      PortfolioValuationServiceImpl serviceSpy = spy(valuationService);

      Portfolio portfolio = mock(Portfolio.class);
      Account goodAcc = createMockAccount(USD, HUNDRED_USD, Map.of());
      Account faultyAcc = createMockAccount(USD, HUNDRED_USD, Map.of());

      when(goodAcc.getState()).thenReturn(AccountLifecycleState.ACTIVE);
      when(faultyAcc.getState()).thenReturn(AccountLifecycleState.ACTIVE);
      when(goodAcc.getAccountCurrency()).thenReturn(USD);
      when(portfolio.getAccounts()).thenReturn(List.of(goodAcc, faultyAcc));

      doReturn(HUNDRED_USD).when(serviceSpy).calculateAccountValue(eq(goodAcc), any());
      doReturn(null).when(serviceSpy).calculateAccountValue(eq(faultyAcc), any());

      when(exchangeRateService.convert(HUNDRED_USD, USD)).thenReturn(HUNDRED_USD);

      Money total = serviceSpy.calculateTotalValue(portfolio, USD, Map.of());

      assertThat(total).isEqualTo(HUNDRED_USD);
      verify(exchangeRateService, times(1)).convert(any(), eq(USD));
    }
  }

  @Nested
  @DisplayName("Account and Position Valuations")
  class AccountValuationTests {
    @Test
    @DisplayName("calculateAccountValue: returns zero for inactive accounts")
    void calculateAccountValueReturnsZeroForInactive() {
      Account inactiveAccount = mock(Account.class);
      when(inactiveAccount.isActive()).thenReturn(false);
      when(inactiveAccount.getAccountCurrency()).thenReturn(USD);

      Money result = valuationService.calculateAccountValue(inactiveAccount, Map.of());

      assertThat(result).isEqualTo(Money.zero(USD));
    }

    @Test
    @DisplayName("calculatePositionsValue: filters out cash assets")
    void calculatePositionsValueExcludesCashType() {
      Position stockPos = mock(AcbPosition.class);
      Position cashPos = mock(AcbPosition.class);
      when(stockPos.type()).thenReturn(AssetType.STOCK);
      when(cashPos.type()).thenReturn(AssetType.CASH);

      MarketAssetQuote quote = new MarketAssetQuote(AAPL, Price.of("150", USD), null, null, null,
          null, null, null, null, null, "Unit testing source", Instant.now());
      when(stockPos.currentValue(any())).thenReturn(Money.of(150, "USD"));

      Account account = mock(Account.class);
      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.getPositionEntries()).thenReturn(
          Set.of(Map.entry(AAPL, stockPos), Map.entry(new AssetSymbol("CASH"), cashPos)));

      Money result = valuationService.calculatePositionsValue(account, Map.of(AAPL, quote));

      assertThat(result).isEqualTo(Money.of(150, "USD"));
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

      Money result = valuationService.calculatePositionsValue(account, Map.of());

      assertThat(result).isEqualTo(costBasis);
    }

    @Test
    @DisplayName("resolvePositionValue: converts currency when quote currency differs from account")
    void resolvePositionValueConvertsCurrency() {

      Position pos = mock(AcbPosition.class);
      Currency accountCurrency = Currency.of("USD");

      Money priceInCad = Money.of(100, "CAD");
      MarketAssetQuote cadQuote = new MarketAssetQuote(
          AAPL, new Price(priceInCad), null, null, null,
          null, null, null, null, null, "Test Source", Instant.now());

      Account account = createMockAccount(accountCurrency, Money.zero(accountCurrency), Map.of(AAPL, pos));

      Money priceInUsd = Money.of(75, "USD");
      when(exchangeRateService.convert(priceInCad, accountCurrency)).thenReturn(priceInUsd);

      Money expectedFinalValue = Money.of(750, "USD");

      when(pos.currentValue(argThat(p -> p.currency().equals(accountCurrency) && p.amount().doubleValue() == 75.0)))
          .thenReturn(expectedFinalValue);

      Money result = valuationService.calculatePositionsValue(account, Map.of(AAPL, cadQuote));

      assertThat(result).isEqualTo(expectedFinalValue);
      verify(exchangeRateService).convert(priceInCad, accountCurrency);
    }
  }
}