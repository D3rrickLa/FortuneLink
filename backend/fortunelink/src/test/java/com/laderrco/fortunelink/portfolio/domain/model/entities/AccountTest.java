package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AccountTest {
  private static final Currency USD = Currency.of("USD");
  private AccountId accountId;
  private Account account;
  private PositionStrategy strategy;

  @BeforeEach
  void setUp() {
    accountId = AccountId.newId();
    strategy = PositionStrategy.ACB;
    account = new Account(accountId, "Main Investment", AccountType.TAXABLE_INVESTMENT, USD,
        strategy);
  }

  @Nested
  @DisplayName("Constructor and Initialization")
  class CreationTests {
    @Test
    @DisplayName("constructor: initializes account with correct default state")
    void initializesCorrectly() {
      assertAll(() -> assertEquals(accountId, account.getAccountId()),
          () -> assertEquals("Main Investment", account.getName()),
          () -> assertTrue(account.getCashBalance().isZero()), () -> assertTrue(account.isActive()),
          () -> assertEquals(0, account.getPositionCount()),
          () -> assertTrue(account.getAccountType().requiresCapitalGainsTracking()));
    }

    @Test
    @DisplayName("protectedConstructor: maintains partially invalid state for JPA hydration")
    void protectedConstructorState() {
      Account jpaAccount = new Account();
      assertAll(() -> assertNull(jpaAccount.getAccountId()),
          () -> assertNotNull(jpaAccount.getPositionEntries(),
              "Positions must be initialized to avoid NPE"),
          () -> assertNull(jpaAccount.getAccountCurrency()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "  ", "\t" })
    @DisplayName("constructor: throws exception for null or blank names")
    void throwsForInvalidNames(String invalidName) {
      assertThrows(DomainArgumentException.class,
          () -> new Account(accountId, invalidName, AccountType.CHEQUING, USD, strategy));
    }
  }

  @Nested
  @DisplayName("Cash Operation Tests: Deposit, Withdraw, Buy, Sell")
  class CashOperations {
    @Test
    @DisplayName("deposit: increases balance when valid")
    void depositIncreasesBalance() {
      Money amount = Money.of(1000, "USD");

      account.deposit(amount, "Initial Funding");

      assertEquals(amount, account.getCashBalance());
    }

    @Test
    @DisplayName("deposit: rejects negative or wrong currency")
    void depositRejectsInvalidInputs() {
      assertAll(() -> assertThrows(IllegalArgumentException.class,
          () -> account.deposit(Money.of(-100, "USD"), "Invalid")),

          () -> assertThrows(CurrencyMismatchException.class,
              () -> account.deposit(Money.of(100, "EUR"), "Wrong Currency")));
    }

    @Test
    @DisplayName("withdraw: reduces balance when sufficient funds")
    void withdrawReducesBalance() {
      account.deposit(Money.of(500, "USD"), "Funding");

      account.withdraw(Money.of(200, "USD"), "Personal use");

      assertEquals(Money.of(300, "USD"), account.getCashBalance());
    }

    @Test
    @DisplayName("withdraw: rejects invalid inputs and insufficient funds")
    void withdrawRejectsInvalidCases() {
      account.deposit(Money.of(100, "USD"), "Funding");

      assertAll(() -> assertThrows(InsufficientFundsException.class,
          () -> account.withdraw(Money.of(200, "USD"), "Too much")),

          () -> assertThrows(IllegalArgumentException.class,
              () -> account.withdraw(Money.of(-50, "USD"), "Negative")),

          // Important: negative should fail BEFORE insufficient funds
          () -> assertThrows(IllegalArgumentException.class,
              () -> account.withdraw(Money.of(-200, "USD"), "Negative + too much")));
    }

    @Test
    @DisplayName("withdraw: allows negative balance when explicitly enabled")
    void withdrawAllowsNegativeWhenEnabled() {
      account.deposit(Money.of(100, "USD"), "Funding");

      account.withdraw(Money.of(200, "USD"), "Overdraft", true);

      assertEquals(Money.of(-100, "USD"), account.getCashBalance());
    }

    @Test
    @DisplayName("applyFee: deducts fee from balance")
    void applyFeeDeductsBalance() {
      account.deposit(Money.of(100, "USD"), "Funding");

      account.applyFee(Money.of(10, "USD"), "Brokerage Fee");

      assertEquals(Money.of(90, "USD"), account.getCashBalance());
    }

    @Test
    @DisplayName("applyFee: rejects invalid inputs and insufficient funds")
    void applyFeeRejectsInvalidCases() {
      assertAll(() -> assertThrows(InsufficientFundsException.class,
          () -> account.applyFee(Money.of(10, "USD"), "Fee")),

          () -> assertThrows(IllegalArgumentException.class,
              () -> account.applyFee(Money.of(-10, "USD"), "Negative Fee")),

          () -> assertThrows(IllegalArgumentException.class,
              () -> account.applyFee(Money.of(10, "USD"), null)),

          () -> assertThrows(IllegalArgumentException.class,
              () -> account.applyFee(Money.of(10, "USD"), "   ")));
    }
  }

  @Nested
  @DisplayName("Realized Gains and History")
  class RealizedGainsManagementTests {
    private final AssetSymbol AAPL = new AssetSymbol("AAPL");
    private final AssetSymbol TSLA = new AssetSymbol("TSLA");

    @BeforeEach
    void setUpGains() {
      account.recordRealizedGain(AAPL, Money.of("100", USD), Money.of("500", USD), Instant.now());
      account.recordRealizedGain(AAPL, Money.of("50", USD), Money.of("200", USD), Instant.now());
      account.recordRealizedGain(TSLA, Money.of("300", USD), Money.of("1000", USD), Instant.now());
    }

    @Test
    @DisplayName("clearRealizedGains: removes only records for the specified symbol and updates timestamp")
    void clearRealizedGainsSucceedsForSpecificSymbol() {
      Instant beforeUpdate = account.getLastUpdatedOn();

      account.clearRealizedGains(AAPL);

      List<RealizedGainRecord> remaining = account.getRealizedGains();

      assertAll(() -> assertEquals(1, remaining.size()),
          () -> assertEquals(TSLA, remaining.get(0).symbol()), () -> assertTrue(
              account.getLastUpdatedOn().isAfter(beforeUpdate) || account.getLastUpdatedOn()
                  .equals(beforeUpdate)));
    }

    @Test
    @DisplayName("clearRealizedGains: does nothing when symbol does not exist")
    void clearRealizedGainsDoesNothingWhenSymbolNotFound() {
      int initialSize = account.getRealizedGains().size();

      account.clearRealizedGains(new AssetSymbol("MSFT"));

      assertEquals(initialSize, account.getRealizedGains().size());
    }

    @Test
    @DisplayName("clearRealizedGains: throws when symbol is null")
    void clearRealizedGainsFailsWhenSymbolIsNull() {
      assertThrows(DomainArgumentException.class, () -> account.clearRealizedGains(null));
    }

    @Test
    @DisplayName("clearAllRealizedGains: removes all records and resets totals")
    void clearAllRealizedGainsSucceeds() {
      assertFalse(account.getRealizedGains().isEmpty());

      account.clearAllRealizedGains();

      assertAll(() -> assertTrue(account.getRealizedGains().isEmpty()),
          () -> assertEquals(Money.zero(USD), account.getTotalRealizedGainLoss()));
    }

    @Test
    @DisplayName("getTotalRealizedGainLoss: aggregates all gain records correctly")
    void getTotalRealizedGainLossReturnsCorrectTotal() {
      assertEquals(Money.of(450, "USD"), account.getTotalRealizedGainLoss());
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = { "NON_REGISTERED_INVESTMENT", "MARGIN",
        "TAXABLE_INVESTMENT" })
    @DisplayName("accountType: identifies types that require capital gains tracking")
    void accountTypeRequiresCapitalGainsTracking(AccountType type) {
      Account testAccount = new Account(accountId, "test", type, USD, strategy);

      assertTrue(testAccount.getAccountType().requiresCapitalGainsTracking());
    }
  }

  @Nested
  @DisplayName("Position Management")
  class PositionManagement {
    private final AssetSymbol apple = new AssetSymbol("AAPL");
    private Position emptyPosition;

    @BeforeEach
    void setup() {
      emptyPosition = AcbPosition.empty(apple, AssetType.STOCK, USD);
    }

    @Test
    @DisplayName("updatePosition: adds or removes positions based on quantity")
    void updatesOrRemovesPosition() {
      Position pos = AcbPosition.empty(apple, AssetType.STOCK, USD)
          .buy(Quantity.of(10), Money.of(150, "USD"), Instant.now()).getUpdatedPosition();

      account.updatePosition(apple, pos);
      assertThat(account.hasPosition(apple)).isTrue();
      assertThat(account.getPositionCount()).isEqualTo(1);
      assertThat(account.getPositionEntries().size()).isEqualTo(1);
      assertThat(account.getPosition(apple)).isNotNull();

      // Zero quantity should remove the position
      Position emptyPos = AcbPosition.empty(apple, AssetType.STOCK, USD);
      account.updatePosition(apple, emptyPos);
      assertThat(account.hasPosition(apple)).isFalse();
    }

    @Test
    @DisplayName("updatePositoin: Throws when we try to update a position with wrong symbol")
    void updatePositionSymbolMismatch() {
      // emptyPosition is for AAPL
      AssetSymbol google = new AssetSymbol("GOOGL");
      assertThrows(IllegalArgumentException.class,
          () -> account.updatePosition(google, emptyPosition));
    }

    @Test
    @DisplayName("ensurePosition: creates correct implementation based on strategy")
    void ensurePositionStrategy() {
      Position acbPos = account.ensurePosition(apple, AssetType.STOCK);
      assertInstanceOf(AcbPosition.class, acbPos);

      Account fifoAccount = new Account(accountId, "FIFO", AccountType.CHEQUING, USD,
          PositionStrategy.FIFO);
      assertInstanceOf(FifoPosition.class, fifoAccount.ensurePosition(apple, AssetType.STOCK));
    }

    @ParameterizedTest
    @EnumSource(value = PositionStrategy.class, names = { "LIFO", "SPECIFIC_ID" })
    @DisplayName("ensurePosition: throws exception for unsupported strategies")
    void unsupportedStrategies(PositionStrategy unsupported) {
      Account badAccount = new Account(accountId, "Bad", AccountType.CHEQUING, USD, unsupported);
      assertThrows(IllegalArgumentException.class,
          () -> badAccount.ensurePosition(apple, AssetType.STOCK));
    }

    @Test
    @DisplayName("clearPosition: clears said position, apple, when it is found in account")
    void clearPositionClearsWhenSymbolFound() {
      account.clearPosition(apple);
      assertTrue(account.getPosition(apple).isEmpty());
    }

    @Nested
    class PositionStrategyTest {

      @Test
      void testEnsurePosition_CreatesAcb_WhenStrategyIsAcb() {
        // GIVEN: An account initialized with ACB strategy
        Currency cad = Currency.of("CAD");
        PositionStrategy strategy = PositionStrategy.ACB;
        Account cadAccount = new Account(AccountId.newId(), "CAD Account", AccountType.CHEQUING,
            cad, strategy);
        AssetSymbol symbol = new AssetSymbol("TD.TO");

        // WHEN: Creating a new position
        Position position = cadAccount.ensurePosition(symbol, AssetType.STOCK);

        // THEN: It should be an AcbPosition
        // System.out.println("Account strategy: " + cadAccount.getPositionStrategy());
        // Position position2 = cadAccount.ensurePosition(symbol, AssetType.STOCK);
        // System.out.println("Created position class: " +
        // position2.getClass().getName());
        assertInstanceOf(AcbPosition.class, position,
            "Account with ACB strategy must produce AcbPosition");
      }

      @Test
      void testEnsurePosition_CreatesFifo_WhenStrategyIsFifo() {
        // GIVEN: An account initialized with FIFO strategy
        Currency USD = Currency.of("USD");
        PositionStrategy strategy = PositionStrategy.FIFO;
        Account usdAccount = new Account(AccountId.newId(), "USD Account", AccountType.CHEQUING,
            USD, strategy);
        AssetSymbol symbol = new AssetSymbol("MSFT");

        // WHEN: Creating a new position
        Position position = usdAccount.ensurePosition(symbol, AssetType.STOCK);

        // THEN: It should be a FifoPosition
        assertInstanceOf(FifoPosition.class, position,
            "Account with FIFO strategy must produce FifoPosition");
        assertFalse(usdAccount.getAccountType().requiresCapitalGainsTracking());
      }

      @ParameterizedTest
      @EnumSource(value = PositionStrategy.class, names = { "LIFO", "SPECIFIC_ID" })
      void testCreateEmptyPosition_Fails_OnUnsupportedStrategy(PositionStrategy strategy) {
        // GIVEN: An account with a strategy not covered by the switch (e.g.,
        // SPECIFIC_ID)
        // Note: You may need to mock the enum or use a value that exists in the Enum
        // but not the switch
        PositionStrategy unsupported = strategy;
        Account errorAccount = new Account(AccountId.newId(), "Error", AccountType.CHEQUING, USD,
            unsupported);

        // WHEN/THEN: Expect the IllegalArgumentException you saw earlier
        assertThrows(IllegalArgumentException.class,
            () -> errorAccount.ensurePosition(new AssetSymbol("AAPL"), AssetType.STOCK),
            "Should throw exception for unhandled position strategies");
      }

      @Test
      void testEnsurePosition_ReturnsExistingPosition_WithoutChangingType() {
        AssetSymbol apple = new AssetSymbol("AAPL");
        // Create an initial position via 'buy' to put it in the map
        Position activePosition = AcbPosition.empty(apple, AssetType.STOCK, USD)
            .buy(new Quantity(BigDecimal.TEN), Money.of(100, "USD"), Instant.now())
            .getUpdatedPosition();

        account.updatePosition(apple, activePosition);

        // WHEN: Ensuring the position exists
        Position retrieved = account.ensurePosition(apple, AssetType.STOCK);

        // THEN: It returns the existing one, not a new empty one
        assertEquals(activePosition.totalQuantity(), retrieved.totalQuantity());
        assertEquals(1, account.getPositionCount());
      }
    }
  }

  @Nested
  class AccountLifecycle {
    @Test
    @DisplayName("close: prevents closing when account is not empty")
    void closeFailsWhenAccountIsNotEmpty() {
      account.deposit(Money.of(10, "USD"), "Leftover");
      assertThrows(IllegalStateException.class, () -> account.close());

      account.beginReplay();
      account.endReplay();

      AssetSymbol symbol = new AssetSymbol("AAPL");
      Position pos = mock(AcbPosition.class);
      when(pos.symbol()).thenReturn(symbol);
      when(pos.totalQuantity()).thenReturn(new Quantity(BigDecimal.ONE));
      account.updatePosition(symbol, pos);

      assertThrows(IllegalStateException.class, () -> account.close());
    }

    @Test
    @DisplayName("close: successfully closes account when empty")
    void closeSucceedsWhenAccountIsEmpty() {
      account.beginReplay();
      account.endReplay();

      account.close();

      assertFalse(account.isActive());
      assertNotNull(account.getCloseDate());
    }

    @Test
    @DisplayName("reopen: handles valid and invalid scenarios")
    void reopenBehavior() {
      assertThrows(IllegalStateException.class, () -> account.reopen());
      account.beginReplay();
      account.endReplay();

      account.close();
      account.reopen();

      assertTrue(account.isActive());
      assertNull(account.getCloseDate());
    }

    @Test
    @DisplayName("operations: prevent actions when account is closed")
    void operationsFailWhenAccountIsClosed() {
      account.close();

      assertThrows(AccountClosedException.class,
          () -> account.deposit(Money.of(10, "USD"), "Late"));
    }

    @Test
    @DisplayName("updateName: successfully updates account name")
    void updateNameSucceedsWithValidName() {
      account.updateName("new Name");
      assertEquals("new Name", account.getName());
    }

    @NullSource
    @EmptySource
    @ParameterizedTest
    @ValueSource(strings = { "  ", "\t", "\n" })
    @DisplayName("updateName: rejects null, empty, or blank names")
    void updateNameFailsWithInvalidName(String invalidName) {
      assertThrows(IllegalArgumentException.class, () -> account.updateName(invalidName));
    }

    @Test
    @DisplayName("positions: clears all positions")
    void clearAllPositionsSucceeds() {
      // REPLACEMENT: beginReplay() clears positions internally
      account.beginReplay();
      assertEquals(0, account.getPositionCount());
      account.endReplay();
    }

    @Test
    @DisplayName("cash: resets balance to zero")
    void resetCashToZeroSucceeds() {
      account.beginReplay();
      assertEquals(Money.zero("USD"), account.getCashBalance());
      account.endReplay();
    }

    @Test
    @DisplayName("healthStatus: transitions correctly")
    void healthStatusTransitionsCorrectly() {
      account.markStale();
      account.restoreHealth();
      assertFalse(account.isStale());
    }
  }

  @Nested
  @DisplayName("Getters and Encapsulation")
  class GettersAndEncapsulation {
    @Test
    @DisplayName("getAllPositions: returns an unmodifiable collection of position copies")
    void getAllPositionsReturnsUnmodifiableCopy() {
      AssetSymbol apple = new AssetSymbol("AAPL");
      Position pos = mock(AcbPosition.class);
      when(pos.symbol()).thenReturn(apple);
      when(pos.totalQuantity()).thenReturn(new Quantity(BigDecimal.ONE));
      when(pos.copy()).thenReturn(pos);

      account.updatePosition(apple, pos);
      Collection<Position> positions = account.getAllPositions();

      assertAll(() -> assertEquals(1, positions.size()),
          () -> assertThrows(UnsupportedOperationException.class, positions::clear,
              "The returned collection should be immutable"));
    }

    @Test
    @DisplayName("getPosition: returns empty optional when symbol is not found")
    void getPositionReturnsEmptyWhenMissing() {
      assertThat(account.getPosition(new AssetSymbol("NONE"))).isEmpty();
    }

    @Test
    @DisplayName("getRealizedGainsFor: filters records correctly by symbol and identifies gain/loss")
    void getRealizedGainsForFiltersBySymbol() {
      AssetSymbol apple = new AssetSymbol("AAPL");
      AssetSymbol msft = new AssetSymbol("MSFT");

      account.recordRealizedGain(apple, Money.of("100", USD), Money.of("500", USD), Instant.now());
      account.recordRealizedGain(apple, Money.of("50", USD), Money.of("200", USD), Instant.now());
      account.recordRealizedGain(msft, Money.of("-300", USD), Money.of("1000", USD), Instant.now());

      List<RealizedGainRecord> appleRecords = account.getRealizedGainsFor(apple);
      List<RealizedGainRecord> msftRecords = account.getRealizedGainsFor(msft);

      assertAll(() -> assertThat(appleRecords.size()).isEqualTo(2),
          () -> assertThat(appleRecords.get(0).isGain()).isTrue(),
          () -> assertThat(msftRecords.size()).isEqualTo(1),
          () -> assertThat(msftRecords.get(0).isLoss()).isTrue());
    }

    @Nested
    @DisplayName("Financial Checks")
    class FinancialChecks {
      @Test
      @DisplayName("hasSufficientCash: correctly validates balance and currency matching")
      void hasSufficientCashValidation() {
        account.deposit(Money.of(100, "USD"), "Initial deposit");
        Money exactAmount = Money.of(100, "USD");
        Money tooMuch = Money.of(101, "USD");
        Money wrongCurrency = Money.of(50, "EUR");

        assertAll(() -> assertTrue(account.hasSufficientCash(exactAmount),
            "Should be true for exact balance"),
            () -> assertFalse(account.hasSufficientCash(tooMuch),
                "Should be false for insufficient balance"),
            () -> assertThrows(CurrencyMismatchException.class,
                () -> account.hasSufficientCash(wrongCurrency)));
      }
    }
  }
}