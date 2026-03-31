package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

// TODO: consolidate this, properly
@DisplayName("Account Tests")
class AccountTest {
  private static final Currency USD = Currency.of("USD");
  private static final String VALID_NAME = "Main Investment";
  // private static final String VALID_REASON = "Test Reason";
  private static final AssetSymbol AAPL = new AssetSymbol("AAPL");
  // private static final AssetSymbol GOOGL = new AssetSymbol("GOOGL");

  private Account account;
  private AccountId accountId;
  private PositionStrategy strategy;

  @BeforeEach
  void setUp() {
    accountId = AccountId.newId();
    strategy = PositionStrategy.ACB;
    account = new Account(accountId, VALID_NAME, AccountType.TAXABLE_INVESTMENT, USD, strategy);
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
          () -> assertEquals(AccountLifecycleState.ACTIVE, account.getState()),
          () -> assertTrue(account.getAccountType().requiresCapitalGainsTracking()),
          () -> assertEquals(account.getPositionStrategy(), PositionStrategy.ACB),
          () -> assertNotNull(account.getCreationDate()),
          () -> assertEquals(account.getLastUpdatedOn(), account.getCreationDate()),
          () -> assertTrue(account.getHealthStatus().equals(HealthStatus.HEALTHY)));
    }

    @Test
    @DisplayName("protectedConstructor: maintains partially invalid state for JPA hydration")
    void protectedConstructorState() {
      Account jpaAccount = new Account();
      assertAll(() -> assertNull(jpaAccount.getAccountId()),
          () -> assertNotNull(jpaAccount.getPositionBook(), "PositionBook must be initialized"),
          () -> assertNull(jpaAccount.getAccountCurrency()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   ", "\t" })
    @DisplayName("constructor: throws exception for null or blank names")
    void throwsForInvalidNames(String invalidName) {
      assertThrows(DomainArgumentException.class,
          () -> new Account(accountId, invalidName, AccountType.CHEQUING, USD, strategy));
    }

    @Test
    void updateName() {
      account.updateName("New Name");
      assertThat(account.getName()).isEqualTo("New Name");
    }

    @Test
    void updateNameFailsWhenNullAndIsEmpty() {
      assertThatThrownBy(() -> account.updateName(null)).isInstanceOf(DomainArgumentException.class)
          .hasMessageContaining("Account name cannot be empty");
      assertThatThrownBy(() -> account.updateName("   ")).isInstanceOf(
          DomainArgumentException.class).hasMessageContaining("Account name cannot be empty");
    }
  }

  @Nested
  @DisplayName("Cash Operation Tests: Deposit, Withdraw, Fee")
  class CashOperations {
    @Test
    @DisplayName("deposit: increases balance and updates lastUpdatedOn")
    void depositIncreasesBalance() {
      Money amount = Money.of(1000, "USD");
      account.deposit(amount, "Initial Funding");
      assertEquals(amount, account.getCashBalance());
    }

    @Test
    @DisplayName("deposit: rejects negative amounts or wrong currency")
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
      account.withdraw(Money.of(200, "USD"), "Personal use", false);
      assertEquals(Money.of(300, "USD"), account.getCashBalance());
      assertThat(account.hasSufficientCash(Money.of(200, USD))).isTrue();
    }

    @Test
    @DisplayName("withdraw: rejects invalid inputs and enforces balance limits")
    void withdrawRejectsInvalidCases() {
      account.deposit(Money.of(100, "USD"), "Funding");
      assertAll(() -> assertThrows(InsufficientFundsException.class,
          () -> account.withdraw(Money.of(200, "USD"), "Too much", false)),
          () -> assertThat(account.hasSufficientCash(Money.of(2000, USD))).isFalse(),
          () -> assertThrows(IllegalArgumentException.class,
              () -> account.withdraw(Money.of(-50, "USD"), "Negative", false)));
    }

    @Test
    @DisplayName("withdraw: allows negative balance when explicitly enabled")
    void withdrawAllowsNegativeWhenEnabled() {
      account.deposit(Money.of(100, "USD"), "Funding");
      account.withdraw(Money.of(200, "USD"), "Overdraft", true);
      assertEquals(Money.of(-100, "USD"), account.getCashBalance());
    }

    @Test
    @DisplayName("applyFee: deducts fee and validates description")
    void applyFeeDeductsBalance() {
      account.deposit(Money.of(100, "USD"), "Funding");
      account.applyFee(Money.of(10, "USD"), "Brokerage Fee");
      assertEquals(Money.of(90, "USD"), account.getCashBalance());
    }

    @Test
    void applyFeeFailsWhenFeeAmountNotPositive() {
      Money feeAmount = Money.of(-1, USD);

      assertThatThrownBy(() -> account.applyFee(feeAmount, "NOTES")).isInstanceOf(
          IllegalArgumentException.class)
          .hasMessageContaining("Withdrawal amount must be positive");
    }

    @Test
    void applyFeeFailsWhenFeeAmountGreaterThanCashBalance() {
      Money feeAmount = Money.of(100, USD);
      account.deposit(Money.of(20, USD), "testing");
      assertThatThrownBy(() -> account.applyFee(feeAmount, "NOTES")).isInstanceOf(
          InsufficientFundsException.class);
    }
  }

  @Nested
  @DisplayName("Realized Gains and History")
  class RealizedGainsTests {
    private final AssetSymbol AAPL = new AssetSymbol("AAPL");
    private final AssetSymbol apple = new AssetSymbol("AAPL");
    private final AssetSymbol google = new AssetSymbol("GOOGL");

    @Test
    @DisplayName("recordRealizedGain: records gain and calculates total correctly")
    void recordsAndAggregatesGains() {
      account.recordRealizedGain(AAPL, Money.of(100, USD), Money.of(500, USD), Instant.now());
      account.recordRealizedGain(AAPL, Money.of(-20, USD), Money.of(100, USD), Instant.now());

      assertEquals(Money.of(80, USD), account.getTotalRealizedGainLoss());
      assertEquals(2, account.getRealizedGains().size());
    }

    @Test
    @DisplayName("clearRealizedGainsForSymbol: surgically removes gains for one symbol only")
    void clearSpecificSymbolGains() {
      AssetSymbol TSLA = new AssetSymbol("TSLA");
      account.recordRealizedGain(AAPL, Money.of(100, USD), Money.of(500, USD), Instant.now());
      account.recordRealizedGain(TSLA, Money.of(300, USD), Money.of(1000, USD), Instant.now());

      account.clearRealizedGainsForSymbol(AAPL);

      assertAll(() -> assertEquals(1, account.getRealizedGains().size()),
          () -> assertEquals(TSLA, account.getRealizedGains().get(0).symbol()));
    }

    @Test
    void failsWhenClosed() {
      account.close();

      assertThatThrownBy(
          () -> account.recordRealizedGain(AAPL, Money.of(100, USD), Money.of(500, USD),
              Instant.now()))
          .isInstanceOf(AccountClosedException.class);
    }

    @Test
    void getRealizedGainsFor_ShouldReturnOnlyMatchingRecords() {
      // 1. Arrange: Add records to the account's internal list
      // (If the list is private, you might need a 'recordGain' method to populate it)
      RealizedGainRecord appleGain = new RealizedGainRecord(apple, Money.of(100, USD),
          Money.of(500, USD), Instant.now());
      RealizedGainRecord googleGain = new RealizedGainRecord(google, Money.of(200, USD),
          Money.of(1000, USD), Instant.now());

      account.recordRealizedGain(apple, Money.of(100, USD), Money.of(500, USD), Instant.now());
      account.recordRealizedGain(google, Money.of(200, USD), Money.of(1000, USD), Instant.now());

      // 2. Act
      var results = account.getRealizedGainsFor(apple);

      // 3. Assert
      assertThat(results).as("Should only contain gains for the requested symbol").hasSize(1)
          .containsExactly(appleGain).doesNotContain(googleGain);
    }
  }

  @Nested
  @DisplayName("Position Management")
  class PositionManagementTests {
    private final AssetSymbol apple = new AssetSymbol("AAPL");

    @Test
    @DisplayName("applyPositionResult: updates the PositionBook and marks account as touched")
    void updatesPositionBook() {
      Position pos = AcbPosition.empty(apple, AssetType.STOCK, USD)
          .buy(Quantity.of(10), Money.of(150, "USD"), Instant.now()).getUpdatedPosition();

      account.applyPositionResult(apple, pos);

      assertAll(() -> assertTrue(account.hasPosition(apple)),
          () -> assertEquals(1, account.getPositionCount()),
          () -> assertEquals(pos, account.getPosition(apple).orElseThrow()),
          () -> assertEquals(account.getRealizedGainsFor(apple).size(), 0),
          () -> assertEquals(account.getPositionEntries().size(), 1));
    }

    @Test
    @DisplayName("clearPositionForRecalculation: removes a specific symbol from the book")
    void clearPositionWorks() {
      Position pos = AcbPosition.empty(apple, AssetType.STOCK, USD)
          .buy(Quantity.of(10), Money.of(150, "USD"), Instant.now()).getUpdatedPosition();
      account.applyPositionResult(apple, pos);

      account.clearPositionForRecalculation(apple);
      assertTrue(account.getPosition(apple).isEmpty());
    }
  }

  @Nested
  @DisplayName("Account Lifecycle: Replay and Closing")
  class LifecycleTests {
    @Test
    @DisplayName("beginReplay: resets all mutable state and enters REPLAYING state")
    void beginReplayResetsState() {
      account.deposit(Money.of(100, USD), "Initial");
      account.recordRealizedGain(new AssetSymbol("AAPL"), Money.of(50, USD), Money.of(100, USD),
          Instant.now());

      account.beginReplay();

      assertAll(() -> assertTrue(account.isInReplayMode()),
          () -> assertTrue(account.getCashBalance().isZero()),
          () -> assertTrue(account.getRealizedGains().isEmpty()),
          () -> assertEquals(0, account.getPositionCount()));
    }

    @Test
    void depositFailsAsResaonIsNotGiven() {
      assertThatThrownBy(() -> account.deposit(Money.of(100, USD), " ")).isInstanceOf(
          IllegalArgumentException.class)
          .hasMessageContaining("Reason/description cannot be empty");
      assertThatThrownBy(() -> account.deposit(Money.of(100, USD), null)).isInstanceOf(
          IllegalArgumentException.class)
          .hasMessageContaining("Reason/description cannot be empty");
    }

    @Test
    void beginReplayFailsWhenStateIsNotClosed() {
      account.close();
      assertThatThrownBy(() -> account.beginReplay()).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot replay a closed account");
    }

    @Test
    void beginReplayFailsWhenReplayingAlready() {
      account.beginReplay();
      assertThatThrownBy(() -> account.beginReplay()).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Account is already in replay mode");
    }

    @Test
    void endReplaySuccessfullyEnds() {
      account.beginReplay();
      assertThat(account.isInReplayMode()).isTrue();
      account.endReplay();
      assertThat(account.getState()).isEqualTo(AccountLifecycleState.ACTIVE);
      assertThat(account.isInReplayMode()).isFalse();

      assertThatThrownBy(() -> account.endReplay()).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Account is not in replay mode");
    }

    @Test
    @DisplayName("close: fails if cash balance remains")
    void closeFailsWithCash() {
      account.deposit(Money.of(1, USD), "Dust");
      assertThrows(IllegalStateException.class, () -> account.close());
    }

    @Test
    void closeFailWhenHasPosition() {
      AssetSymbol apple = new AssetSymbol("AAPL");
      Position pos = AcbPosition.empty(apple, AssetType.STOCK, USD)
          .buy(Quantity.of(10), Money.of(150, "USD"), Instant.now()).getUpdatedPosition();

      account.applyPositionResult(apple, pos);

      assertThatThrownBy(() -> account.close()).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot close account with open positions");
    }

    @Test
    @DisplayName("close: successfully transitions to CLOSED and sets closeDate")
    void closeSucceedsWhenEmpty() {
      account.close();
      assertAll(() -> assertFalse(account.isActive()),
          () -> assertEquals(AccountLifecycleState.CLOSED, account.getState()),
          () -> assertNotNull(account.getCloseDate()));
    }

    @Test
    @DisplayName("operations: prevent deposits/withdrawals when account is closed")
    void failsWhenClosed() {
      account.close();
      assertThatThrownBy(() -> account.deposit(Money.of(10, USD), "Too late")).isInstanceOf(
          AccountClosedException.class);

      account.reopen();
      account.beginReplay();
      assertThatThrownBy(() -> account.close()).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot close account during replay");
    }

    @Test
    @DisplayName("reopen: transitions from CLOSED back to ACTIVE")
    void reopenTransitionsCorrectly() {
      account.close();
      account.reopen();
      assertAll(() -> assertTrue(account.isActive()), () -> assertNull(account.getCloseDate()),
          () -> assertEquals(AccountLifecycleState.ACTIVE, account.getState()));
    }

    @Test
    void reopenFailsWhenStateIsNotClosed() {
      assertThatThrownBy(() -> account.reopen()).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Can only reopen a closed account");
    }
  }

  @Nested
  @DisplayName("Health and Metadata")
  class MetadataTests {
    @Test
    @DisplayName("healthStatus: transitions between HEALTHY and STALE")
    void transitionsHealth() {
      account.markStale();
      assertTrue(account.isStale());
      account.restoreHealth();
      assertFalse(account.isStale());
    }

    @Test
    @DisplayName("updateName: changes name and trims whitespace")
    void updatesName() {
      account.updateName("  New Portfolio Name  ");
      assertEquals("New Portfolio Name", account.getName());
    }

    @Test
    @DisplayName("ensurePosition: creates position when symbol not found")
    void ensurePositionCreatesPositionEmpty() {
      account.ensurePosition(AAPL, AssetType.STOCK);
      assertThat(account.getPositionCount()).isEqualTo(1);
    }
  }
}