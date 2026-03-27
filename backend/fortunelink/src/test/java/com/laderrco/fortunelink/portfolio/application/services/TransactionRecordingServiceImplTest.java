package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionRecordingServiceImplTest {

  @InjectMocks
  private TransactionRecordingServiceImpl service;

  @Mock
  private Account account;

  private static final AssetSymbol AAPL = new AssetSymbol("AAPL");
  private static final Currency USD = Currency.of("USD");
  private static final Instant NOW = Instant.parse("2026-03-26T10:00:00Z");
  private static final Instant CREATION_DATE = NOW.minusSeconds(86400);
  private static final Quantity TEN = new Quantity(new BigDecimal("10"));
  private static final Price HUNDRED_USD_PRICE = new Price(new Money(new BigDecimal("100"), USD));
  private static final Money HUNDRED_USD_MONEY = new Money(new BigDecimal("100.00"), USD);
  private static final String NOTES = "Test transaction";

  @BeforeEach
  void setUp() {
    lenient().when(account.isActive()).thenReturn(true);
    lenient().when(account.getCreationDate()).thenReturn(CREATION_DATE);
    lenient().when(account.getAccountCurrency()).thenReturn(USD);
    lenient().when(account.getAccountId()).thenReturn(AccountId.newId());
  }

  @Nested
  @DisplayName("Validation and Lifecycle")
  class ValidationTests {
    @Test
    @DisplayName("recordBuy: throw AccountClosedException when account is inactive")
    void recordBuyThrowsWhenAccountClosed() {
      when(account.isActive()).thenReturn(false);
      assertThatThrownBy(
          () -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW))
          .isInstanceOf(AccountClosedException.class);
    }

    @Test
    @DisplayName("recordDeposit: throw IllegalArgumentException for transaction before account creation")
    void recordDepositThrowsForInvalidDate() {
      Instant invalidDate = CREATION_DATE.minus(Duration.ofDays(1));
      assertThatThrownBy(() -> service.recordDeposit(account, HUNDRED_USD_MONEY, NOTES, invalidDate))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Trade Operations (Buy/Sell)")
  class TradeOperationsTests {
    private static Stream<Arguments> buyFeeProvider() {
      return Stream.of(
          Arguments.of(Named.of("Null Fees", null), new BigDecimal("1000.00")),
          Arguments.of(Named.of("Empty Fees", List.of()), new BigDecimal("1000.00")),
          Arguments.of(Named.of("With $5 Fee", List.of(Fee.of(FeeType.BROKERAGE, Money.of(5, USD), NOW))),
              new BigDecimal("1005.00")));
    }

    private static Stream<Arguments> sellFeeProvider() {
      return Stream.of(
          Arguments.of(Named.of("No Fees", null), new BigDecimal("1000.00")),
          Arguments.of(Named.of("With $10 Fee", List.of(Fee.of(FeeType.COMMISSION, Money.of(10, USD), NOW))),
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
      Position mockPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, HUNDRED_USD_MONEY, NOW, NOW);
      when(account.hasSufficientCash(any())).thenReturn(true);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(mockPos));

      service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, fees, NOTES, NOW);

      verify(account).ensurePosition(AAPL, AssetType.STOCK);
      verify(account, atLeastOnce()).withdraw(
          argThat(money -> money.amount().compareTo(expectedTotal) == 0),
          contains("BUY " + AAPL.symbol()),
          eq(false));

      verify(account).applyPositionResult(eq(AAPL), any());
    }

    @Test
    @DisplayName("recordBuy: throw InsufficientFundsException and skip mutation when cash is low")
    void recordBuyInsufficientFundsFailsGracefully() {
      when(account.hasSufficientCash(any())).thenReturn(false);
      when(account.getCashBalance()).thenReturn(Money.zero(USD));

      assertThatThrownBy(
          () -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW))
          .isInstanceOf(InsufficientFundsException.class);

      verify(account, never()).withdraw(any(), any(), anyBoolean());
      verify(account, never()).applyPositionResult(any(), any());
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
      verify(account).deposit(
          argThat(m -> m.amount().compareTo(expectedNetDeposit) == 0),
          contains(AAPL.symbol()));

      verify(account).recordRealizedGain(
          eq(AAPL),
          argThat(m -> m.amount().compareTo(expectedGain) == 0), // Dynamic gain check
          argThat(m -> m.amount().compareTo(initialCostBasis) == 0), // Basis Sold
          eq(NOW));
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
      verify(account).applyPositionResult(eq(AAPL), argThat(pos -> pos.totalQuantity().isZero() &&
          pos.totalCostBasis().amount().compareTo(BigDecimal.ZERO) == 0));

      // Verify the gain is exactly (1000.00 - 500.123456789)
      BigDecimal expectedGain = new BigDecimal("1000.00").subtract(weirdBasis);
      verify(account).recordRealizedGain(
          eq(AAPL),
          argThat(m -> m.amount().compareTo(expectedGain) == 0),
          argThat(m -> m.amount().compareTo(weirdBasis) == 0),
          eq(NOW));
    }

    @Test
    @DisplayName("recordSell: throw InsufficientQuantityException when selling more than held")
    void recordSellOversellThrowsError() {
      Position smallPos = new AcbPosition(AAPL, AssetType.STOCK, USD, new Quantity(new BigDecimal("5")),
          HUNDRED_USD_MONEY, CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(smallPos));

      assertThatThrownBy(() -> service.recordSell(account, AAPL, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW))
          .isInstanceOf(InsufficientQuantityException.class);
    }
  }

  @Nested
  @DisplayName("Income and Corporate Actions")
  class CorporateActionTests {
    private static final Quantity ONE = Quantity.of(1);

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

      Transaction tx = service.recordDividendReinvestment(account, AAPL, TEN, HUNDRED_USD_PRICE, NOTES, NOW);

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
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, ONE, lowBasis, CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));

      // ROC of $100 (1 share * $100 price) vs Basis of $10
      service.recordReturnOfCapital(account, AAPL, ONE, HUNDRED_USD_PRICE, NOTES, NOW);

      // Verify the Gain is exactly $90.00
      verify(account).recordRealizedGain(
          eq(AAPL),
          argThat(m -> m.amount().compareTo(new BigDecimal("90")) == 0),
          argThat(m -> m.amount().compareTo(BigDecimal.ZERO) == 0),
          eq(NOW));
    }

    @Test
    @DisplayName("recordReturnOfCapital: throw IllegalArgumentException on partial quantity")
    void recordROCRequiresFullQuantity() {
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, HUNDRED_USD_MONEY, CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));

      Quantity partialQty = new Quantity(new BigDecimal("5.00"));
      assertThatThrownBy(() -> service.recordReturnOfCapital(account, AAPL, partialQty, HUNDRED_USD_PRICE, NOTES, NOW))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must match total held quantity");
    }
  }

  @Nested
  @DisplayName("Cash and Replay Operations")
  class CashAndReplayTests {
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

    @Test
    @DisplayName("replayFullTransaction: invoke begin and end replay on account")
    void replayFullTransactionTriggersLifecycle() {
      List<Transaction> history = List.of();
      service.replayFullTransaction(account, history);

      verify(account).beginReplay();
      verify(account).endReplay();
    }

    @Test
    @DisplayName("replayTransaction: throw IllegalStateException if called during full replay")
    void replayTransactionFailsDuringFullReplay() {
      when(account.isInReplayMode()).thenReturn(true);
      Price price = Price.of("100", USD);
      Money delta = Money.of(-1000, "USD");
      Transaction tx = buildTx(TransactionType.BUY, price, TEN, delta, List.of());

      assertThatThrownBy(() -> service.replayTransaction(account, tx))
          .isInstanceOf(IllegalStateException.class);
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
    @DisplayName("recordFee: withdraw amount and verify negative cash delta")
    void recordFeeSuccess() {
      Money feeAmt = Money.of(15, USD);
      Transaction tx = service.recordFee(account, feeAmt, NOTES, NOW);

      assertThat(tx.cashDelta().isNegative()).isTrue();
      verify(account).withdraw(eq(feeAmt), contains("FEE: 15"), eq(false));
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

    @Test
    @DisplayName("recordReturnOfCapital: throw IllegalStateException when position is missing")
    void recordROCThrowsWhenNoPosition() {
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.recordReturnOfCapital(account, AAPL, TEN, HUNDRED_USD_PRICE, NOTES, NOW))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  private Transaction buildTx(TransactionType type, Price price, Quantity quantity, Money delta, List<Fee> fees) {
    return Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(type)
        .execution(new TradeExecution(AAPL, quantity, price))
        .cashDelta(delta)
        .fees(fees != null ? fees : List.of())
        .notes(NOTES)
        .metadata(TransactionMetadata.manual(AssetType.STOCK))
        .occurredAt(NOW)
        .build();
  }
}