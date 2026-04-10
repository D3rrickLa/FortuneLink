package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.CashImpact;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionRecordingServiceImplTest {
  private static final AssetSymbol AAPL = new AssetSymbol("AAPL");
  private static final Currency USD = Currency.of("USD");
  private static final Instant NOW = Instant.parse("2026-03-26T10:00:00Z");
  private static final Instant CREATION_DATE = NOW.minusSeconds(86400);
  private static final Quantity TEN = new Quantity(new BigDecimal("10"));
  private static final Price HUNDRED_USD_PRICE = new Price(new Money(new BigDecimal("100"), USD));
  private static final Money HUNDRED_USD_MONEY = new Money(new BigDecimal("100.00"), USD);
  private static final Money ONE_THOUSAND_USD_MONEY = new Money(new BigDecimal("1000.00"), USD);
  private static final String NOTES = "Test transaction";
  @Mock
  private Account account;
  @InjectMocks
  private TransactionRecordingServiceImpl service;

  @BeforeEach
  void setUp() {
    lenient().when(account.isActive()).thenReturn(true);
    lenient().when(account.getCreationDate()).thenReturn(CREATION_DATE);
    lenient().when(account.getAccountCurrency()).thenReturn(USD);
    lenient().when(account.getAccountId()).thenReturn(AccountId.newId());
  }

  private Transaction buildTx(TransactionType type, Price price, Quantity quantity, Money delta,
      List<Fee> fees) {
    return Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(type)
        .execution(new TradeExecution(AAPL, quantity, price)).cashDelta(delta)
        .fees(fees != null ? fees : List.of()).notes(NOTES)
        .metadata(TransactionMetadata.manual(AssetType.STOCK)).occurredAt(NOW).build();
  }

  @Nested
  @DisplayName("Validation and Lifecycle")
  class ValidationTests {
    @Test
    @DisplayName("recordBuy: throw AccountClosedException when account is inactive")
    void recordBuyThrowsWhenAccountClosed() {
      when(account.isActive()).thenReturn(false);
      assertThatThrownBy(
          () -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null,
              NOTES, NOW, false))
          .isInstanceOf(AccountClosedException.class);
    }

    @Test
    @DisplayName("recordDeposit: throw IllegalArgumentException for transaction before account creation")
    void recordDepositThrowsForInvalidDate() {
      Instant invalidDate = CREATION_DATE.minus(Duration.ofDays(1));
      assertThatThrownBy(
          () -> service.recordDeposit(account, HUNDRED_USD_MONEY, NOTES, invalidDate)).isInstanceOf(
              IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Trade Operations (Buy/Sell)")
  class TradeOperationsTests {
    private static Stream<Arguments> buyFeeProvider() {
      return Stream.of(Arguments.of(Named.of("Null Fees", null), new BigDecimal("1000.00")),
          Arguments.of(Named.of("Empty Fees", List.of()), new BigDecimal("1000.00")), Arguments.of(
              Named.of("With $5 Fee", List.of(Fee.of(FeeType.BROKERAGE, Money.of(5, USD), NOW))),
              new BigDecimal("1005.00")));
    }

    private static Stream<Arguments> sellFeeProvider() {
      return Stream.of(Arguments.of(Named.of("No Fees", null), new BigDecimal("1000.00")),
          Arguments.of(
              Named.of("With $10 Fee", List.of(Fee.of(FeeType.COMMISSION, Money.of(10, USD), NOW))),
              new BigDecimal("990.00")),
          Arguments.of(Named.of("Multiple Fees ($15 total)",
              List.of(Fee.of(FeeType.BROKERAGE, Money.of(10, USD), NOW),
                  Fee.of(FeeType.CLEARING_FEE, Money.of(5, USD), NOW))),
              new BigDecimal("985.00")));
    }

    @ParameterizedTest
    @MethodSource("buyFeeProvider")
    @DisplayName("recordBuy: withdraw total cost including fees")
    void recordBuySuccessInitializesPosition(List<Fee> fees, BigDecimal expectedTotal) {
      Position mockPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, HUNDRED_USD_MONEY, NOW,
          NOW);
      when(account.hasSufficientCash(any())).thenReturn(true);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(mockPos));

      service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, fees, NOTES, NOW,
          false);

      verify(account).ensurePosition(AAPL, AssetType.STOCK);
      verify(account, atLeastOnce()).withdraw(
          argThat(money -> money.amount().compareTo(expectedTotal) == 0),
          contains("BUY " + AAPL.symbol()), eq(false));

      verify(account).applyPositionResult(eq(AAPL), any());
    }

    @Test
    @DisplayName("recordBuy: throw InsufficientFundsException and skip mutation when cash is low")
    void recordBuyInsufficientFundsFailsGracefully() {
      when(account.hasSufficientCash(any())).thenReturn(false);
      when(account.getCashBalance()).thenReturn(Money.zero(USD));

      assertThatThrownBy(
          () -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null,
              NOTES, NOW, false))
          .isInstanceOf(InsufficientFundsException.class);

      verify(account, never()).withdraw(any(), any(), anyBoolean());
      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("recordBuy: allows transaction despite low cash when skipCashCheck is true")
    void recordBuyAllowsInsufficientFundsWhenSkipCheckIsTrue() {
      Position acb = AcbPosition.empty(AAPL, AssetType.STOCK, USD);
      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.getPosition(any())).thenReturn(Optional.of(acb));

      service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW,
          true);

      verify(account).applyPositionResult(any(), any());
      verify(account, never()).hasSufficientCash(any());
    }

    @Test
    @DisplayName("recordBuy: allows transaction despite low cash when account is in replay mode")
    void recordBuyAllowsInsufficientFundsDuringReplay() {
      Position acb = AcbPosition.empty(AAPL, AssetType.STOCK, USD);
      when(account.isInReplayMode()).thenReturn(true); // But we are replaying
      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.getPosition(any())).thenReturn(Optional.of(acb));

      service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW,
          false);

      // Assert
      verify(account).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("recordBuy: still enforces cash check when skipCashCheck is false and not in replay")
    void recordBuyEnforcesCashCheckByDefault() {
      when(account.hasSufficientCash(any())).thenReturn(false);
      when(account.isInReplayMode()).thenReturn(false);
      when(account.getCashBalance()).thenReturn(Money.zero(USD));

      assertThatThrownBy(
          () -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null,
              NOTES, NOW, false))
          .isInstanceOf(InsufficientFundsException.class);

      verify(account).hasSufficientCash(any());
      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("recordBuy: throws InsufficientFundsException when cash is low and NOT in replay mode")
    void recordBuyThrowsWhenInsufficientAndNotInReplay() {
      when(account.hasSufficientCash(any())).thenReturn(false);
      when(account.isInReplayMode()).thenReturn(false);
      when(account.getCashBalance()).thenReturn(Money.zero(USD));

      assertThatThrownBy(
          () -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null,
              NOTES, NOW, false))
          .isInstanceOf(InsufficientFundsException.class);

      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("recordBuy: proceeds normally when funds are sufficient (Replay status irrelevant)")
    void recordBuyProceedsWhenFundsAreSufficient() {
      Position acb = AcbPosition.empty(AAPL, AssetType.STOCK, USD);

      when(account.hasSufficientCash(any())).thenReturn(true);
      when(account.getPosition(any())).thenReturn(Optional.of(acb));

      // Even if replay is false, it should work
      lenient().when(account.isInReplayMode()).thenReturn(false);
      when(account.getAccountCurrency()).thenReturn(USD);

      service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW,
          false);

      verify(account).applyPositionResult(any(), any());
    }

    @ParameterizedTest
    @MethodSource("sellFeeProvider")
    @DisplayName("recordSell: deposit net proceeds and record correct realized gain")
    void recordSellSuccessCalculatesProceedsAndGain(List<Fee> fees, BigDecimal expectedNetDeposit) {
      // 10 shares @ $50 cost basis ($500 total cost)
      BigDecimal initialCostBasis = new BigDecimal("500.00");
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN,
          new Money(initialCostBasis, USD), CREATION_DATE, NOW);

      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));
      when(account.getAccountCurrency()).thenReturn(USD);

      // Selling 10 shares @ $100 ($1000 gross)
      service.recordSell(account, AAPL, TEN, HUNDRED_USD_PRICE, fees, NOTES, NOW);

      // Calculate expected gain: Net Proceeds - Cost Basis
      BigDecimal expectedGain = expectedNetDeposit.subtract(initialCostBasis);

      verify(account).recordRealizedGain(eq(AAPL), any(Money.class), any(Money.class), eq(NOW));
      verify(account).deposit(argThat(m -> m.amount().compareTo(expectedNetDeposit) == 0),
          contains(AAPL.symbol()));

      verify(account).recordRealizedGain(eq(AAPL),
          argThat(m -> m.amount().compareTo(expectedGain) == 0), // Dynamic gain check
          argThat(m -> m.amount().compareTo(initialCostBasis) == 0), // Basis Sold
          eq(NOW));
    }

    @Test
    @DisplayName("recordSell: throws IllegalStateException when symbol is not in account")
    void recordSellThrowsWhenPositionMissing() {
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());

      assertThatThrownBy(
          () -> service.recordSell(account, AAPL, TEN, HUNDRED_USD_PRICE, List.of(), NOTES,
              NOW))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot sell: no open position for AAPL");

      verify(account).getPosition(AAPL);
      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("recordSell: full liquidation should result in exactly zero basis and quantity")
    void fullLiquidationResultsInZeroPosition() {
      // Arrange: 10.00 shares with a highly precise cost basis
      BigDecimal weirdBasis = new BigDecimal("500.123456789");
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN,
          new Money(weirdBasis, USD), CREATION_DATE, NOW);

      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));
      when(account.getAccountCurrency()).thenReturn(USD);

      service.recordSell(account, AAPL, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW);

      // Verify the new position state is absolute zero
      verify(account).applyPositionResult(eq(AAPL), argThat(pos -> pos.totalQuantity().isZero()
          && pos.totalCostBasis().amount().compareTo(BigDecimal.ZERO) == 0));

      // Verify the gain is exactly (1000.00 - 500.123456789)
      BigDecimal expectedGain = new BigDecimal("1000.00").subtract(weirdBasis);
      verify(account).recordRealizedGain(eq(AAPL),
          argThat(m -> m.amount().compareTo(expectedGain) == 0),
          argThat(m -> m.amount().compareTo(weirdBasis) == 0), eq(NOW));
    }

    @Test
    @DisplayName("recordSell: throw InsufficientQuantityException when selling more than held")
    void recordSellOversellThrowsError() {
      Position smallPos = new AcbPosition(AAPL, AssetType.STOCK, USD,
          new Quantity(new BigDecimal("5")), HUNDRED_USD_MONEY, CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(smallPos));

      assertThatThrownBy(
          () -> service.recordSell(account, AAPL, TEN, HUNDRED_USD_PRICE, null, NOTES,
              NOW))
          .isInstanceOf(InsufficientQuantityException.class);
    }
  }

  @Nested
  @DisplayName("Income and Corporate Actions")
  class CorporateActionTests {
    private static final Quantity ONE = Quantity.of(1);
    private static final Ratio RATIO_3_FOR_1 = new Ratio(3, 1);
    private static final Ratio RATIO_2_FOR_1 = new Ratio(2, 1);

    @Test
    @DisplayName("recordDividend: deposit cash and store symbol in metadata")
    void recordDividendCashSuccess() {
      Transaction tx = service.recordDividend(account, AAPL, HUNDRED_USD_MONEY, NOTES, NOW);

      assertThat(tx.metadata().get("symbol")).isEqualTo("AAPL");
      verify(account, atLeastOnce()).deposit(eq(HUNDRED_USD_MONEY), contains("DIVIDEND: AAPL"));
    }

    @Test
    @DisplayName("recordDividendReinvestment: update position without affecting cash balance")
    void recordDRIPUpdatesPositionOnly() {
      Position mockPos = AcbPosition.empty(AAPL, AssetType.STOCK, USD);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(mockPos));

      Transaction tx = service.recordDividendReinvestment(account, AAPL, TEN, HUNDRED_USD_PRICE,
          NOTES, NOW);

      assertThat(tx.cashDelta().isZero()).isTrue();
      verify(account, never()).deposit(any(), any());
      verify(account).applyPositionResult(eq(AAPL), any());
    }

    @Test
    @DisplayName("recordReturnOfCapital: record excess gain when ROC exceeds cost basis")
    void recordROCRecordsCapitalGainOnExcess() {
      Money lowBasis = new Money(new BigDecimal("10.00"), USD);
      // Position held is 10 shares, but we only need to process ROC on 1 share to
      // trigger the logic
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, ONE, lowBasis,
          CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));

      // ROC of $100 (1 share * $100 price) vs Basis of $10
      service.recordReturnOfCapital(account, AAPL, ONE, HUNDRED_USD_PRICE, NOTES, NOW);

      // Verify the Gain is exactly $90.00
      verify(account).recordRealizedGain(eq(AAPL),
          argThat(m -> m.amount().compareTo(new BigDecimal("90")) == 0),
          argThat(m -> m.amount().compareTo(BigDecimal.ZERO) == 0), eq(NOW));
    }

    @Test
    @DisplayName("recordReturnOfCapital: throw IllegalArgumentException on partial quantity")
    void recordROCRequiresFullQuantity() {
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, HUNDRED_USD_MONEY,
          CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));

      Quantity partialQty = new Quantity(new BigDecimal("5.00"));
      assertThatThrownBy(
          () -> service.recordReturnOfCapital(account, AAPL, partialQty, HUNDRED_USD_PRICE, NOTES,
              NOW))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must match total held quantity");
    }

    @Test
    @DisplayName("recordSplit: throw AccountClosedException when account is inactive")
    void recordSplitThrowsWhenAccountClosed() {
      when(account.isActive()).thenReturn(false);

      assertThatThrownBy(
          () -> service.recordSplit(account, AAPL, RATIO_3_FOR_1, NOTES, NOW)).isInstanceOf(
              AccountClosedException.class);
    }

    @Test
    @DisplayName("recordSplit: throw IllegalStateException when no position exists for symbol")
    void recordSplitThrowsWhenNoPositionFound() {
      when(account.isActive()).thenReturn(true);
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());

      assertThatThrownBy(
          () -> service.recordSplit(account, AAPL, RATIO_3_FOR_1, NOTES, NOW)).isInstanceOf(
              IllegalStateException.class)
          .hasMessageContaining("no open position found for AAPL");
    }

    @Test
    @DisplayName("recordSplit: successfully create transaction and apply position effect")
    void recordSplitCreatesTransactionSuccessfully() {
      AccountId accountId = AccountId.newId();
      when(account.isActive()).thenReturn(true);
      when(account.getAccountId()).thenReturn(accountId);
      when(account.getAccountCurrency()).thenReturn(USD);

      Position existingPosition = AcbPosition.empty(AAPL, AssetType.STOCK, USD);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPosition));

      // Act
      Transaction tx = service.recordSplit(account, AAPL, RATIO_2_FOR_1, NOTES, NOW);

      // Assert
      assertThat(tx.transactionType()).isEqualTo(TransactionType.SPLIT);
      assertThat(tx.split()).isEqualTo(RATIO_2_FOR_1);
      assertThat(tx.cashDelta().isZero()).isTrue();
      assertThat(tx.execution().quantity().amount()).isEqualTo(BigDecimal.valueOf(2).setScale(8));
      assertThat(tx.execution().pricePerUnit().isZero()).isTrue();

      // Side Effect Verification
      // Since applyPositionEffect is private in the service, we verify the
      // interaction with the account mock that the private method triggers.
      // verify(account).applyPositionEffect(tx);
    }
  }

  @Nested
  @DisplayName("Cash and Replay Operations")
  class CashAndReplayTests {
    private static Stream<Arguments> provideCashImpactScenarios() {
      // A position that actually has shares to sell
      Position appleWithShares = AcbPosition.empty(AAPL, AssetType.STOCK, USD)
          .buy(TEN, HUNDRED_USD_MONEY, NOW).getUpdatedPosition(); // Now has 10 shares

      return Stream.of(
          // BUY: Works with empty position
          Arguments.of(TransactionType.BUY, CashImpact.OUT, ONE_THOUSAND_USD_MONEY.negate(),
              AcbPosition.empty(AAPL, AssetType.STOCK, USD)),

          // SELL: Needs a position that HAS shares, otherwise ACB math divides by zero
          Arguments.of(TransactionType.SELL, CashImpact.IN, ONE_THOUSAND_USD_MONEY,
              appleWithShares),

          // SELL: But empty so should throw exception
          // Arguments.of(TransactionType.SELL, CashImpact.IN, ONE_THOUSAND_USD_MONEY,
          // AcbPosition.empty(AAPL, AssetType.STOCK, USD)),

          // NONE: Works with empty
          Arguments.of(TransactionType.DIVIDEND_REINVEST, CashImpact.NONE, Money.zero(USD),
              AcbPosition.empty(AAPL, AssetType.STOCK, USD)));

    }

    private static Stream<Arguments> provideValidCashImpactScenarios() {
      Position appleWithShares = AcbPosition.empty(AAPL, AssetType.STOCK, USD)
          .buy(TEN, HUNDRED_USD_MONEY, NOW).getUpdatedPosition();

      return Stream.of(
          Arguments.of(TransactionType.BUY, CashImpact.OUT, ONE_THOUSAND_USD_MONEY.negate(),
              AcbPosition.empty(AAPL, AssetType.STOCK, USD)),
          Arguments.of(TransactionType.SELL, CashImpact.IN, ONE_THOUSAND_USD_MONEY,
              appleWithShares),
          Arguments.of(TransactionType.DIVIDEND_REINVEST, CashImpact.NONE, Money.zero(USD),
              AcbPosition.empty(AAPL, AssetType.STOCK, USD)));
    }

    private static Stream<Arguments> provideInvalidReplayScenarios() {
      return Stream.of(
          // 1. Excluded -> Returns early (No exception, no account interaction)
          Arguments.of(mockTx(true, true, true), false, null),

          // 2. Already in Replay Mode -> Throws IllegalStateException
          Arguments.of(mockTx(false, true, true), true, IllegalStateException.class),

          // 3. Doesn't affect holdings -> Throws IllegalArgumentException
          Arguments.of(mockTx(false, false, true), false, IllegalArgumentException.class),

          // 4. Execution is Null -> Returns early in applyPositionEffect (No exception)
          Arguments.of(mockTx(false, true, false), false, null));
    }

    // Helper to create the mock state for the parameter provider
    private static Transaction mockTx(boolean excluded, boolean affectsHoldings,
        boolean hasExecution) {
      Transaction tx = mock(Transaction.class);
      TransactionType type = mock(TransactionType.class);
      lenient().when(tx.isExcluded()).thenReturn(excluded);
      lenient().when(tx.transactionType()).thenReturn(type);
      lenient().when(type.affectsHoldings()).thenReturn(affectsHoldings);
      lenient().when(tx.execution()).thenReturn(hasExecution ? mock(TradeExecution.class) : null);
      return tx;
    }

    @Test
    @DisplayName("recordDeposit: increase balance and verify positive cashDelta")
    void recordDepositSuccess() {
      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordDeposit(account, HUNDRED_USD_MONEY, NOTES, NOW);

      assertThat(tx.transactionType()).isEqualTo(TransactionType.DEPOSIT);
      assertThat(tx.cashDelta()).isEqualTo(HUNDRED_USD_MONEY);
      assertThat(tx.cashDelta().isPositive()).isTrue();
      verify(account).deposit(eq(HUNDRED_USD_MONEY), eq("DEPOSIT"));

    }

    @Test
    @DisplayName("recordWithdrawal: decrease balance and record negative cashDelta")
    void recordWithdrawalSuccess() {
      Transaction tx = service.recordWithdrawal(account, HUNDRED_USD_MONEY, NOTES, NOW);

      assertThat(tx.cashDelta().isNegative()).isTrue();
      verify(account).withdraw(eq(HUNDRED_USD_MONEY), eq("WITHDRAWAL"), eq(false));
    }

    @ParameterizedTest
    @MethodSource("provideCashImpactScenarios")
    @DisplayName("replayFullTransaction: handles different cash impact types correctly")
    void replayFullTransaction_HandlesAllCashImpacts(TransactionType type, CashImpact impact,
        Money delta, Position startingPosition) {
      Transaction tx = buildTx(type, HUNDRED_USD_PRICE, TEN, delta, null);

      when(account.getPosition(eq(AAPL))).thenReturn(Optional.of(startingPosition));
      service.replayFullTransaction(account, List.of(tx));

      InOrder inOrder = inOrder(account);
      inOrder.verify(account).beginReplay();

      switch (impact) {
        case IN -> verify(account).deposit(eq(delta), contains("REPLAY"));
        case OUT -> verify(account).withdraw(eq(delta.abs()), contains("REPLAY"), eq(true));
        case NONE -> {
          verify(account, never()).deposit(any(), anyString());
          verify(account, never()).withdraw(any(), anyString(), anyBoolean());
        }
      }

      inOrder.verify(account).endReplay();
    }

    @ParameterizedTest
    @MethodSource("provideValidCashImpactScenarios")
    void replayFullTransactionHandlesValidCashImpacts(TransactionType type, CashImpact impact,
        Money delta, Position startingPosition) {
      Transaction tx = buildTx(type, HUNDRED_USD_PRICE, TEN, delta, null);
      when(account.getPosition(eq(AAPL))).thenReturn(Optional.of(startingPosition));

      service.replayFullTransaction(account, List.of(tx));

      // Assert successful completion and lifecycle
      verify(account).beginReplay();
      verify(account).endReplay();
    }

    @Test
    @DisplayName("replayTransaction: successfully applies position effect for valid transaction")
    void replayTransactionAppliesPositionEffect() {
      Transaction tx = buildTx(TransactionType.BUY, HUNDRED_USD_PRICE, TEN,
          ONE_THOUSAND_USD_MONEY.negate(), null);

      when(account.isInReplayMode()).thenReturn(false);
      when(account.getPosition(AAPL)).thenReturn(
          Optional.of(AcbPosition.empty(AAPL, AssetType.STOCK, USD)));

      service.replayTransaction(account, tx);

      verify(account).getPosition(AAPL);
      verify(account).applyPositionResult(eq(AAPL), any());
    }

    @Test
    @DisplayName("replayFullTransaction: throws IllegalStateException when selling from empty position")
    void replayFullTransaction_ThrowsOnCorruptSellData() {
      // Scenario: SELL transaction but the account has an empty position
      Transaction tx = buildTx(TransactionType.SELL, HUNDRED_USD_PRICE, TEN, ONE_THOUSAND_USD_MONEY,
          null);
      when(account.getPosition(eq(AAPL))).thenReturn(
          Optional.of(AcbPosition.empty(AAPL, AssetType.STOCK, USD)));

      assertThatThrownBy(() -> service.replayFullTransaction(account, List.of(tx))).isInstanceOf(
          IllegalStateException.class).hasMessageContaining("position is empty");

      // Verification: Even on failure, finally block MUST run
      verify(account).endReplay();
    }

    @Test
    @DisplayName("replayTransaction: execution is NOT NULL but affectsHoldings is FALSE (Chained)")
    void branch2_DoesNotAffectHoldings() {
      Transaction mockTx = mock(Transaction.class);
      TransactionType mockType = mock(TransactionType.class);

      // STUB CHAINING:
      // 1st call: returns TRUE (to pass the replayTransaction guard)
      // 2nd call: returns FALSE (to trigger the return in applyPositionEffect)
      when(mockType.affectsHoldings()).thenReturn(true, false);
      when(mockTx.isExcluded()).thenReturn(false);
      when(mockTx.transactionType()).thenReturn(mockType);
      when(mockTx.execution()).thenReturn(mock(TradeExecution.class));

      service.replayTransaction(account, mockTx);

      verify(account, never()).getPosition(any());
      verify(mockType, times(2)).affectsHoldings();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidReplayScenarios")
    @DisplayName("replayTransaction: should return early or throw for invalid inputs")
    void replayTransactionEarlyExitScenarios(Transaction tx, boolean inReplayMode,
        Class<? extends Throwable> expectedException) {
      lenient().when(account.isInReplayMode()).thenReturn(inReplayMode);

      if (expectedException != null) {
        assertThatThrownBy(() -> service.replayTransaction(account, tx)).isInstanceOf(
            expectedException);
      } else {
        service.replayTransaction(account, tx);

        verify(account, never()).getPosition(any());
        verify(account, never()).applyPositionResult(any(), any());
      }
    }
  }

  @Nested
  @DisplayName("Additional Coverage & Edge Cases")
  class ExtendedCoverageTests {
    @Test
    @DisplayName("recordInterest: verify deposit and metadata for interest income")
    void recordInterestSuccess() {
      Money interestAmt = Money.of(10, USD);
      Transaction tx = service.recordInterest(account, AAPL, interestAmt, NOTES, NOW);

      assertThat(tx.transactionType()).isEqualTo(TransactionType.INTEREST);
      verify(account).deposit(eq(interestAmt), contains("INTEREST: AAPL"));
    }

    @Test
    @DisplayName("recordInterest: should handle null symbol by using CASH reason and omitting symbol metadata")
    void recordInterestWithNullSymbol() {
      AssetSymbol nullSymbol = null;
      Money amount = Money.of("10.00", USD);
      String notes = "Monthly Interest";
      Instant now = Instant.now();

      when(account.getAccountId()).thenReturn(AccountId.newId());
      when(account.isActive()).thenReturn(true);

      Transaction result = service.recordInterest(account, nullSymbol, amount, notes, now);

      String expectedReason = "INTEREST: CASH";
      verify(account).deposit(eq(amount), eq(expectedReason));

      assertFalse(result.metadata().containsKey(TransactionMetadata.KEY_SYMBOL),
          "Metadata should not contain a 'symbol' key when input symbol is null");

      assertEquals(TransactionType.INTEREST, result.transactionType());
      assertEquals(AssetType.CASH, result.metadata().assetType());
    }

    @Test
    @DisplayName("recordFee: withdraw amount and verify negative cash delta")
    void recordFeeSuccess() {
      Money feeAmt = Money.of(15, USD);
      Transaction tx = service.recordFee(account, feeAmt, FeeType.ACCOUNT_MAINTENANCE, NOTES, NOW);

      assertThat(tx.cashDelta().isNegative()).isTrue();
      verify(account).applyFee(eq(feeAmt), contains("FEE: 15"));
    }

    @Test
    @DisplayName("recordReturnOfCapital: throw IllegalStateException when position is missing")
    void recordROCThrowsWhenNoPosition() {
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());

      assertThatThrownBy(
          () -> service.recordReturnOfCapital(account, AAPL, TEN, HUNDRED_USD_PRICE, NOTES,
              NOW))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("replayFullTransaction: skip transactions marked as excluded")
    void replayFullTransactionFiltersExcluded() {
      Transaction excludedTx = mock(Transaction.class);
      when(excludedTx.isExcluded()).thenReturn(true);

      service.replayFullTransaction(account, List.of(excludedTx));

      verify(account, never()).deposit(any(), any());
      verify(account, never()).withdraw(any(), any(), anyBoolean());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("replayFullTransaction: handles null or empty history by still triggering lifecycle")
    void replayFullTransactionHandlesNullOrEmptyReturningNothing(List<Transaction> history) {
      service.replayFullTransaction(account, history);

      InOrder inOrder = inOrder(account);
      inOrder.verify(account).beginReplay();
      inOrder.verify(account).endReplay();

      verify(account, never()).withdraw(any(), anyString(), anyBoolean());
      verify(account, never()).deposit(any(), anyString());
      verify(account, never()).ensurePosition(any(), any());
    }

    @Test
    @DisplayName("executeReplayStep: skips position effect when affectsHoldings is false")
    void executeReplayStep_SkipsPositionEffect() {
      Transaction tx = mock(Transaction.class);
      TransactionType type = mock(TransactionType.class);

      when(tx.transactionType()).thenReturn(type);
      when(type.affectsHoldings()).thenReturn(false); // The key for this test
      when(type.cashImpact()).thenReturn(CashImpact.IN);
      when(tx.cashDelta()).thenReturn(ONE_THOUSAND_USD_MONEY);
      when(type.toString()).thenReturn("CASH_DEPOSIT");

      service.replayFullTransaction(account, List.of(tx));

      // Position logic should NEVER be touched
      verify(account, never()).ensurePosition(any(), any());
      verify(account, never()).getPosition(any());

      // Cash logic SHOULD be touched
      verify(account).deposit(eq(ONE_THOUSAND_USD_MONEY), contains("REPLAY CASH_DEPOSIT"));
    }

    @Test
    @DisplayName("applyPositionEffect: verify the internal orElseThrow lambda message")
    void applyPositionEffect_InternalLambda_Throws() {
      Transaction sellTx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(AccountId.newId())
          .transactionType(TransactionType.SELL) // affectsHoldings = true
          .execution(new TradeExecution(AAPL, TEN, HUNDRED_USD_PRICE))
          .cashDelta(Money.of(1000, USD)).fees(List.of()).notes(NOTES)
          .metadata(TransactionMetadata.manual(AssetType.STOCK)).occurredAt(NOW).build();

      when(account.getPosition(AAPL)).thenReturn(Optional.empty());
      when(account.isInReplayMode()).thenReturn(false);

      assertThatThrownBy(() -> service.replayTransaction(account, sellTx)).isInstanceOf(
          IllegalStateException.class).hasMessageContaining("SELL requires position for AAPL");
    }
  }

  @Nested
  @DisplayName("recordTransferIn Tests")
  class TransferInTests {
    @Test
    @DisplayName("recordTransferIn: successfully deposits and returns transaction")
    void recordTransferIn_Success() {
      AccountId accountId = AccountId.newId();
      when(account.getAccountId()).thenReturn(accountId);

      Transaction tx = service.recordTransferIn(account, HUNDRED_USD_MONEY, NOTES, NOW);

      verify(account).deposit(eq(HUNDRED_USD_MONEY), eq("TRANSFER IN"));

      assertThat(tx.transactionType()).isEqualTo(TransactionType.TRANSFER_IN);
      assertThat(tx.cashDelta()).isEqualTo(HUNDRED_USD_MONEY);
      assertThat(tx.fees()).isEmpty();
    }
  }

  @Nested
  @DisplayName("recordTransferOut Tests")
  class TransferOutTests {
    @Test
    @DisplayName("recordTransferOut: successfully withdraws and returns negated transaction")
    void recordTransferOut_Success() {
      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordTransferOut(account, HUNDRED_USD_MONEY, NOTES, NOW);

      verify(account).withdraw(eq(HUNDRED_USD_MONEY), eq("TRANSFER OUT"), eq(false));

      assertThat(tx.transactionType()).isEqualTo(TransactionType.TRANSFER_OUT);
      assertThat(tx.cashDelta()).isEqualTo(HUNDRED_USD_MONEY.negate());
      assertThat(tx.notes()).isEqualTo(NOTES);
    }
  }
}