package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Account Tests")
class AccountTest {
  private static final Currency USD = Currency.of("USD");
  private static final String VALID_NAME = "Main Investment";
  private static final String VALID_REASON = "Test Reason";

  private static final AssetSymbol AAPL = new AssetSymbol("AAPL");
  private static final AssetSymbol GOOGL = new AssetSymbol("GOOGL");

  private Account account;

  @BeforeEach
  void setUp() {
    account = new Account(AccountId.newId(), VALID_NAME, AccountType.TAXABLE_INVESTMENT, USD,
        PositionStrategy.ACB);
  }

  private Money usd(int amount) {
    return Money.of(amount, USD);
  }

  private void deposit(int amount) {
    account.deposit(usd(amount), VALID_REASON);
  }

  private Position createPosition(AssetSymbol symbol, int qty, int price) {
    return AcbPosition.empty(symbol, AssetType.STOCK, USD)
        .buy(Quantity.of(qty), usd(price), Instant.now()).getUpdatedPosition();
  }

  @Nested
  @DisplayName("Constructor and Initialization")
  class CreationTests {
    @Test
    @DisplayName("initializesCorrectly")
    void initializesCorrectly() {
      assertAll(() -> assertEquals(VALID_NAME, account.getName()),
          () -> assertTrue(account.getCashBalance().isZero()), () -> assertTrue(account.isActive()),
          () -> assertEquals(0, account.getPositionCount()));
    }

    @ParameterizedTest(name = "invalidName: \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("throwsForInvalidNames")
    void throwsForInvalidNames(String invalidName) {
      assertThrows(DomainArgumentException.class,
          () -> new Account(AccountId.newId(), invalidName, AccountType.CHEQUING, USD,
              PositionStrategy.ACB));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("updateNameFailsForInvalidInput")
    void updateNameFailsForInvalidInput(String invalid) {
      assertThatThrownBy(() -> account.updateName(invalid)).isInstanceOf(
          DomainArgumentException.class).hasMessageContaining("Account name cannot be empty");
    }
  }

  @Nested
  @DisplayName("Cash Operations")
  class CashOperations {
    static Stream<Arguments> invalidDeposits() {
      return Stream.of(Arguments.of(Money.of(-100, "USD"), IllegalArgumentException.class),
          Arguments.of(Money.of(100, "EUR"), CurrencyMismatchException.class));
    }

    static Stream<Arguments> invalidWithdrawals() {
      return Stream.of(Arguments.of(Money.of(200, "USD"), InsufficientFundsException.class),
          Arguments.of(Money.of(-50, "USD"), IllegalArgumentException.class));
    }

    @Test
    @DisplayName("depositIncreasesBalance")
    void depositIncreasesBalance() {
      deposit(1000);
      assertEquals(usd(1000), account.getCashBalance());
    }

    @ParameterizedTest
    @DisplayName("depositRejectsInvalidInputs")
    @MethodSource("invalidDeposits")
    void depositRejectsInvalidInputs(Money money, Class<?> exception) {
      assertThatThrownBy(() -> account.deposit(money, VALID_REASON)).isInstanceOf(exception);
    }

    @Test
    @DisplayName("withdrawReducesBalance")
    void withdrawReducesBalance() {
      deposit(500);
      account.withdraw(usd(200), VALID_REASON, false);

      assertEquals(usd(300), account.getCashBalance());
    }

    @ParameterizedTest
    @DisplayName("withdrawRejectsInvalidCases")
    @MethodSource("invalidWithdrawals")
    void withdrawRejectsInvalidCases(Money money, Class<?> exception) {
      deposit(100);

      assertThatThrownBy(() -> account.withdraw(money, VALID_REASON, false)).isInstanceOf(
          exception);
    }

    @ParameterizedTest
    @DisplayName("rejectsInvalidDescriptions")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsInvalidDescriptions(String reason) {
      assertThatThrownBy(() -> account.deposit(usd(100), reason)).isInstanceOf(
              IllegalArgumentException.class)
          .hasMessageContaining("Reason/description cannot be empty");
    }
  }

  @Nested
  @DisplayName("Realized Gains")
  class RealizedGainsTests {
    @Test
    @DisplayName("aggregatesGainsCorrectly")
    void aggregatesGainsCorrectly() {
      account.recordRealizedGain(AAPL, usd(100), usd(500), Instant.now());
      account.recordRealizedGain(AAPL, usd(-20), usd(100), Instant.now());

      assertEquals(usd(80), account.getTotalRealizedGainLoss());
      assertEquals(2, account.getRealizedGains().size());
    }

    @Test
    @DisplayName("filtersGainsBySymbol")
    void filtersGainsBySymbol() {
      account.recordRealizedGain(AAPL, usd(100), usd(500), Instant.now());
      account.recordRealizedGain(GOOGL, usd(200), usd(1000), Instant.now());

      var result = account.getRealizedGainsFor(AAPL);

      assertThat(result).hasSize(1).allMatch(r -> r.symbol().equals(AAPL));
    }
  }

  @Nested
  @DisplayName("Position Management")
  class PositionTests {
    @Test
    @DisplayName("applyPositionUpdatesBook")
    void applyPositionUpdatesBook() {
      Position pos = createPosition(AAPL, 10, 150);

      account.applyPositionResult(AAPL, pos);

      assertAll(() -> assertTrue(account.hasPosition(AAPL)),
          () -> assertEquals(1, account.getPositionCount()));
    }

    @Test
    @DisplayName("clearPositionRemovesSymbol")
    void clearPositionRemovesSymbol() {
      account.applyPositionResult(AAPL, createPosition(AAPL, 10, 150));

      account.clearPositionForRecalculation(AAPL);

      assertTrue(account.getPosition(AAPL).isEmpty());
    }
  }

  @Nested
  @DisplayName("Lifecycle")
  class LifecycleTests {
    @Test
    @DisplayName("beginReplayResetsState")
    void beginReplayResetsState() {
      deposit(100);
      account.recordRealizedGain(AAPL, usd(50), usd(100), Instant.now());

      account.beginReplay();

      assertAll(() -> assertTrue(account.isInReplayMode()),
          () -> assertTrue(account.getCashBalance().isZero()),
          () -> assertTrue(account.getRealizedGains().isEmpty()));
    }

    @Test
    @DisplayName("closeFailsWhenCashExists")
    void closeFailsWhenCashExists() {
      deposit(1);
      assertThrows(IllegalStateException.class, account::close);
    }

    @Test
    @DisplayName("closeSucceedsWhenEmpty")
    void closeSucceedsWhenEmpty() {
      account.close();

      assertAll(() -> assertFalse(account.isActive()),
          () -> assertEquals(AccountLifecycleState.CLOSED, account.getState()));
    }
  }
}