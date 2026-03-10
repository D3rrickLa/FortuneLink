package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioValuationServiceImplTest {

  @Mock
  private ExchangeRateService exchangeRateService;

  @InjectMocks
  private PortfolioValuationServiceImpl valuationService;

  private final Currency USD = Currency.of("USD");
  private final Currency CAD = Currency.of("CAD");

  private PortfolioId portfolioId;
  private UserId userId;

  @BeforeEach
  void setUp() {
    portfolioId = PortfolioId.newId();
    userId = UserId.random();
  }

  @Nested
  class CalculateTotalValue {

    @Test
    void calculateTotalValue_Success_AggregatesMultipleAccountsWithConversion() {
      AssetSymbol apple = new AssetSymbol("AAPL");
      MarketAssetQuote appleQuote = createQuote(apple, 150.0);
      Map<AssetSymbol, MarketAssetQuote> cache = Map.of(apple, appleQuote);

      // Account 1 (USD): $150 (AAPL) + $50 Cash = $200 USD
      Account usdAccount = createAccount(USD, 50.0, List.of(createPosition(apple, 1, 140.0)));

      // Account 2 (CAD): $0 Positions + $100 Cash = $100 CAD
      Account cadAccount = createAccount(CAD, 100.0, List.of());

      Portfolio portfolio = Portfolio.reconstitute(
          portfolioId,
          userId,
          "Portfolio Test",
          "description",
          Map.of(
              usdAccount.getAccountId(), usdAccount,
              cadAccount.getAccountId(), cadAccount),
          Currency.CAD,
          false,
          null,
          null,
          Instant.now(),
          Instant.now());

      // Mock FX: $200 USD -> $200 USD, $100 CAD -> $75 USD
      when(exchangeRateService.convert(any(Money.class), eq(USD)))
          .thenAnswer(inv -> inv.getArgument(0)); // Simple pass-through for USD
      when(exchangeRateService.convert(argThat(m -> m.currency().equals(CAD)), eq(USD)))
          .thenReturn(Money.of(75.0, "USD"));

      // Act
      Money total = valuationService.calculateTotalValue(portfolio, USD, cache);

      // Assert: 200 + 75 = 275
      assertEquals(275.0, total.amount().doubleValue());
      assertEquals(USD, total.currency());
    }

    @Test
    void calculateTotalValue_Success_ReturnsZeroForEmptyPortfolio() {
      Portfolio emptyPortfolio = Portfolio.createNew(userId, "Portfolio name", "", CAD);
      Money result = valuationService.calculateTotalValue(emptyPortfolio, USD, Map.of());

      assertTrue(result.isZero());
      assertEquals(USD, result.currency());
    }
  }

  @Nested
  class CalculateAccountValue {

    @Test
    void calculateAccountValue_Success_CombinesCashAndPositions() {
      AssetSymbol tsla = new AssetSymbol("TSLA");
      Map<AssetSymbol, MarketAssetQuote> cache = Map.of(tsla, createQuote(tsla, 100.0));

      // 2 shares @ $100 + $50 cash = $250
      Account account = createAccount(USD, 50.0, List.of(createPosition(tsla, 2, 180.0)));

      Money result = valuationService.calculateAccountValue(account, cache);

      assertEquals(250.0, result.amount().doubleValue());
    }
  }

  @Nested
  class CalculatePositionsValue {

    @Test
    void calculatePositionsValue_Success_HandlesMixedAccountingMethods() {
      // Arrange
      Account account = new Account(AccountId.newId(), "Mixed", AccountType.TFSA, USD,
          PositionStrategy.ACB);

      AssetSymbol msft = new AssetSymbol("MSFT");
      Position acbPos = new AcbPosition(msft, AssetType.STOCK, USD, Quantity.of(10),
          Money.of(1000, "USD"), Instant.now());

      AssetSymbol aapl = new AssetSymbol("AAPL");
      Position fifoPos = new FifoPosition(aapl, AssetType.STOCK, USD, List.of(
          new TaxLot(Quantity.of(5), Money.of(700, "USD"), Instant.now())));

      account.updatePosition(msft, acbPos);
      account.updatePosition(aapl, fifoPos);

      Map<AssetSymbol, MarketAssetQuote> cache = Map.of(
          msft, createQuote(msft, 150.0), // $1500 value
          aapl, createQuote(aapl, 200.0) // $1000 value
      );

      // Act
      Money result = valuationService.calculatePositionsValue(account, cache);

      // Assert
      assertEquals(2500.0, result.amount().doubleValue());
    }

    @Test
    void calculatePositionsValue_Success_ExcludesCashAssetType() {
      AssetSymbol stockSym = new AssetSymbol("STK");
      AssetSymbol cashSym = new AssetSymbol("CASH");

      Account account = new Account(AccountId.newId(), "FilterTest",
          AccountType.REGISTERED_INVESTMENT, USD,
          PositionStrategy.ACB);

      // Stock position
      account.updatePosition(stockSym,
          new AcbPosition(stockSym, AssetType.STOCK, USD, Quantity.of(1),
              Money.of(100, "USD"), Instant.now()));
      // Cash position (e.g., a Money Market Fund tagged as CASH type)
      account.updatePosition(cashSym,
          new AcbPosition(cashSym, AssetType.CASH, USD, Quantity.of(100),
              Money.of(100, "USD"), Instant.now()));

      Map<AssetSymbol, MarketAssetQuote> cache = Map.of(
          stockSym, createQuote(stockSym, 120.0),
          cashSym, createQuote(cashSym, 1.0));

      Money result = valuationService.calculatePositionsValue(account, cache);

      // Should only include the STOCK (120), ignoring the CASH type position
      assertEquals(120.0, result.amount().doubleValue());
    }

    @Test
    void calculatePositionsValue_Fallback_UsesCostBasisWhenQuoteIsMissing() {
      AssetSymbol msft = new AssetSymbol("MSFT");
      Account account = new Account(AccountId.newId(), "Fallback",
          AccountType.NON_REGISTERED_INVESTMENT, USD,
          PositionStrategy.ACB);

      // Cost basis is $1000
      account.updatePosition(msft,
          new AcbPosition(msft, AssetType.STOCK, USD, Quantity.of(10),
              Money.of(1000, "USD"), Instant.now()));

      // Act: Empty cache
      Money result = valuationService.calculatePositionsValue(account, Map.of());

      // Assert: Returns cost basis
      assertEquals(1000.0, result.amount().doubleValue());
    }

    @Test
    void calculatePositionsValue_Fallback_UsesCostBasisWhenPriceIsZero() {
      AssetSymbol msft = new AssetSymbol("MSFT");
      Account account = new Account(AccountId.newId(), "ZeroPrice",
          AccountType.NON_REGISTERED_INVESTMENT, USD,
          PositionStrategy.ACB);
      account.updatePosition(msft,
          new AcbPosition(msft, AssetType.STOCK, USD, Quantity.of(10),
              Money.of(1000, "USD"), Instant.now()));

      // Quote exists but price is zero
      Map<AssetSymbol, MarketAssetQuote> cache = Map.of(msft, createQuote(msft, 0.0));

      Money result = valuationService.calculatePositionsValue(account, cache);

      assertEquals(1000.0, result.amount().doubleValue());
    }
  }

  // --- Helpers ---

  private MarketAssetQuote createQuote(AssetSymbol symbol, double price) {
    return new MarketAssetQuote(
        symbol,
        new Price(Money.of(price, "USD")),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "TEST",
        Instant.now());

  }

  private Position createPosition(AssetSymbol symbol, int qty, double totalCost) {
    // Note: AcbPosition.empty or a manual constructor works here.
    // Ensure the Currency matches the account currency.
    return new AcbPosition(
        symbol,
        AssetType.STOCK,
        USD,
        Quantity.of(qty),
        Money.of(totalCost, "USD"),
        Instant.now());
  }

  private Account createAccount(Currency currency, double cash, List<Position> positions) {
    // 1. Use the public constructor defined in Account.java
    Account account = new Account(
        AccountId.newId(),
        "Test Account",
        AccountType.CHEQUING, // Default for testing
        currency,
        PositionStrategy.ACB // Matches your AcbPosition usage
    );

    // 2. Set the cash balance.
    // Since cashBalance is private and handled via deposit,
    // we use deposit() to maintain internal state correctly.
    if (cash > 0) {
      account.deposit(Money.of(cash, currency.getCode()), "Initial balance");
    }

    // 3. Use the public updatePosition method to populate the map
    for (Position pos : positions) {
      account.updatePosition(pos.symbol(), pos);
    }

    return account;
  }
}
