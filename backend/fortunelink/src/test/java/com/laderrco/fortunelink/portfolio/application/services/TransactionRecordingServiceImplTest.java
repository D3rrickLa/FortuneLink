package com.laderrco.fortunelink.portfolio.application.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee.FeeMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionRecordingServiceImplTest {
  @InjectMocks
  private TransactionRecordingServiceImpl service;

  @Mock
  private Account account;

  // Constants for repeated test data
  private static final AssetSymbol AAPL = new AssetSymbol("AAPL");
  private static final Currency USD = Currency.of("USD");
  private static final Instant NOW = Instant.parse("2026-03-26T10:00:00Z");
  private static final Instant CREATION_DATE = NOW.minusSeconds(86400);
  private static final Quantity TEN = new Quantity(new BigDecimal("10"));
  private static final Price HUNDRED_USD = new Price(new Money(new BigDecimal("100"), USD));
  private static final String NOTES = "Test transaction";

  @BeforeEach
  void setUp() {
    lenient().when(account.isActive()).thenReturn(true);
    lenient().when(account.getCreationDate()).thenReturn(CREATION_DATE);
    lenient().when(account.getAccountCurrency()).thenReturn(USD);
  }

  private void mockPosition(AssetSymbol symbol, Quantity qty, AssetType type) {
    Position pos = mock(AcbPosition.class);
    lenient().when(pos.totalQuantity()).thenReturn(qty);
    lenient().when(pos.type()).thenReturn(type);
    lenient().when(account.getPosition(symbol)).thenReturn(Optional.of(pos));
    lenient().when(account.getAccountId()).thenReturn(AccountId.newId());
  }

  @Nested
  @DisplayName("Record Buy Operations")
  class RecordBuyTests {
    @Test
    @DisplayName("recordBuy: throw InsufficientFundsException before position mutation")
    void recordBuy_InsufficientFunds_NoMutation() {
      Money required = new Money(new BigDecimal("1000"), USD);
      when(account.hasSufficientCash(required)).thenReturn(false);
      when(account.getCashBalance()).thenReturn(Money.zero(USD));

      assertThatThrownBy(() -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD, null, NOTES, NOW))
          .isInstanceOf(InsufficientFundsException.class);

      // Verify state-changing methods were NEVER called
      verify(account, never()).ensurePosition(any(), any());
      verify(account, never()).withdraw(any(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("recordBuy: create new position and decrement cash including fees")
    void recordBuy_Success_NewPosition() {
      Money feeAmt = new Money(new BigDecimal("5.00"), USD);
      Fee commission = new Fee(FeeType.ACCOUNT_MAINTENANCE, feeAmt, feeAmt,
          ExchangeRate.identity(USD, CREATION_DATE), NOW, new FeeMetadata(Map.of()));
      List<Fee> fees = List.of(commission);

      Money costBasis = new Money(new BigDecimal("500.00"), USD);
      Position realPosition = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, costBasis,
          NOW.minus(Duration.ofDays(30)), NOW);

      // Note: service calls ensurePosition() then getPosition() again.
      // We need to return a position the second time so applyPositionEffect doesn't
      // throw.
      when(account.hasSufficientCash(any())).thenReturn(true);
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());
      when(account.getAccountId()).thenReturn(AccountId.newId());
      when(account.getPosition(AAPL)).thenReturn(Optional.of(realPosition));

      service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD, fees, NOTES, NOW);

      verify(account).ensurePosition(AAPL, AssetType.STOCK);
      verify(account).applyPositionResult(eq(AAPL), any(Position.class));

      Money expectedTotal = new Money(new BigDecimal("1005.00"), USD);
      verify(account).withdraw(
          eq(expectedTotal),
          argThat(s -> s.contains("BUY") && s.contains("AAPL")),
          eq(false));
    }

    @Test
    @DisplayName("applyPositionEffect: records excess capital gain when ROC exceeds cost basis")
    void applyPositionEffect_RecordsExcessGainOnRoc() {
      // 1. Arrange
      // Setup a position with a very LOW cost basis ($10.00)
      Money lowBasis = new Money(new BigDecimal("10.00"), USD);
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, lowBasis, NOW, NOW);

      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));
      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.getAccountId()).thenReturn(AccountId.newId());

      // Create an ROC transaction with a HIGH value ($100.00)
      // This forces the Applier to return an ApplyResult.RocAdjustment with excess
      // gain
      Money rocAmount = new Money(new BigDecimal("100.00"), USD);
      Price priceForRoc = new Price(rocAmount.divide(TEN.amount())); // $10/share

      // 2. Act
      service.recordReturnOfCapital(account, AAPL, TEN, priceForRoc, "Big ROC", NOW);

      // 3. Assert
      // The excess gain should be $90.00 ($100 ROC - $10 Basis)
      Money expectedExcessGain = new Money(new BigDecimal("90.00"), USD);

      verify(account).recordRealizedGain(
          eq(AAPL),
          eq(expectedExcessGain),
          eq(Money.zero(USD)), // ROC gain has zero cost basis sold
          eq(NOW));
    }

    @Test
    @DisplayName("applyPositionEffect: verify the internal orElseThrow lambda message")
    void applyPositionEffect_InternalLambda_Throws() {
      // 1. Arrange
      // We create a real Transaction so we don't have to deal with chained mocks
      Transaction sellTx = Transaction.builder()
          .transactionId(TransactionId.newId())
          .accountId(AccountId.newId())
          .transactionType(TransactionType.SELL) // affectsHoldings = true
          .execution(new TradeExecution(AAPL, TEN, HUNDRED_USD))
          .cashDelta(Money.of(1000, USD))
          .fees(List.of())
          .notes(NOTES)
          .metadata(TransactionMetadata.manual(AssetType.STOCK))
          .occurredAt(NOW)
          .build();

      // Force the empty position
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());
      when(account.isInReplayMode()).thenReturn(false);

      // 2. Act & 3. Assert
      // Using replayTransaction skips the 'recordSell' entry-point validation
      assertThatThrownBy(() -> service.replayTransaction(account, sellTx))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SELL requires position for AAPL");
    }
  }

  @Nested
  @DisplayName("Record Sell Operations")
  class RecordSellTests {
    @Test
    @DisplayName("recordSell: record realized gain and deposit net proceeds")
    void recordSell_Success_CalculatesNet() {
      // Use a REAL Position object so TransactionApplier works
      Money costBasis = new Money(new BigDecimal("500.00"), USD);
      Position realPosition = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, costBasis,
          NOW.minus(Duration.ofDays(30)), NOW);

      when(account.getPosition(AAPL)).thenReturn(Optional.of(realPosition));
      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.getAccountId()).thenReturn(AccountId.newId());

      service.recordSell(account, AAPL, TEN, HUNDRED_USD, null, NOTES, NOW);

      // Assert: 10 shares * $100 price = $1000 deposit
      Money expectedDeposit = new Money(new BigDecimal("1000.00"), USD);
      verify(account).deposit(eq(expectedDeposit), contains("SELL AAPL"));

      // Verify that the service actually pushed the result back to the account
      verify(account).applyPositionResult(eq(AAPL), any(Position.class));

      // Verify realized gain was recorded (since $1000 > $500 cost basis)
      verify(account).recordRealizedGain(eq(AAPL), any(Money.class), any(Money.class), eq(NOW));
    }

    @Test
    @DisplayName("recordSell: record realized gain and deposit net proceeds with fees")
    void recordSell_Success_CalculatesNetWithFee() {
      // Use a REAL Position object so TransactionApplier works
      Money costBasis = new Money(new BigDecimal("500.00"), USD);
      Position realPosition = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, costBasis,
          NOW.minus(Duration.ofDays(30)), NOW);
      Money feeAmount = Money.of(5, USD);
      Fee fee = new Fee(FeeType.ACCOUNT_MAINTENANCE, feeAmount, feeAmount, ExchangeRate.identity(USD, CREATION_DATE),
          CREATION_DATE, new FeeMetadata(Map.of()));

      when(account.getPosition(AAPL)).thenReturn(Optional.of(realPosition));
      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.getAccountId()).thenReturn(AccountId.newId());

      service.recordSell(account, AAPL, TEN, HUNDRED_USD, List.of(fee), NOTES, NOW);

      // Assert: 10 shares * $100 price = $1000 deposit
      Money expectedDeposit = new Money(new BigDecimal("1000.00"), USD).subtract(feeAmount);
      verify(account).deposit(eq(expectedDeposit), contains("SELL AAPL"));

      // Verify that the service actually pushed the result back to the account
      verify(account).applyPositionResult(eq(AAPL), any(Position.class));

      // Verify realized gain was recorded (since $1000 > $500 cost basis)
      verify(account).recordRealizedGain(eq(AAPL), any(Money.class), any(Money.class), eq(NOW));
    }

    @Test
    @DisplayName("recordSell: oversell throws InsufficientQuantityException and does not mutate")
    void recordSell_Oversell_NoMutation() {
      // Position only has 5 shares
      Position smallPosition = new AcbPosition(AAPL, AssetType.STOCK, USD, new Quantity(new BigDecimal("5")),
          Money.of("100", USD), NOW.minus(Duration.ofDays(30)), NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(smallPosition));

      Money feeAmount = Money.of(5, USD);
      Fee fee = new Fee(FeeType.ACCOUNT_MAINTENANCE, feeAmount, feeAmount, ExchangeRate.identity(USD, CREATION_DATE),
          CREATION_DATE, new FeeMetadata(Map.of()));

      assertThatThrownBy(() -> service.recordSell(account, AAPL, TEN, HUNDRED_USD, List.of(fee), NOTES, NOW))
          .isInstanceOf(InsufficientQuantityException.class);

      // State integrity check: No money moved, no positions applied
      verify(account, never()).deposit(any(), anyString());
      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("recordSell: throws IllegalStateException when symbol is not in account")
    void recordSell_ThrowsWhenPositionMissing() {
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.recordSell(account, AAPL, TEN, HUNDRED_USD, List.of(), NOTES, NOW))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot sell: no open position for AAPL");

      verify(account).getPosition(AAPL);
      verify(account, never()).applyPositionResult(any(), any());
    }
  }

  @Nested
  @DisplayName("Dividend and Reinvestment")
  class DividendTests {
    @Test
    @DisplayName("recordDividendReinvestment: cash balance remains unchanged")
    void recordDRIP_NoCashImpact() {
      Position mockPosition = AcbPosition.empty(AAPL, AssetType.STOCK, USD);

      // Ensure account returns this mock when the service asks for it
      when(account.getPosition(AAPL)).thenReturn(Optional.of(mockPosition));
      when(account.getAccountCurrency()).thenReturn(USD);
      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordDividendReinvestment(account, AAPL, TEN, HUNDRED_USD, NOTES, NOW);

      assertThat(tx.cashDelta().isZero()).isTrue();
      verify(account, never()).deposit(any(), any());
      verify(account, never()).withdraw(any(), anyString(), anyBoolean());
      verify(account).applyPositionResult(eq(AAPL), any());
    }

    @Test
    @DisplayName("recordDividend: cash incremented and symbol stored in metadata")
    void recordDividend_CashOnly() {
      Money divAmount = new Money(new BigDecimal("50"), USD);

      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordDividend(account, AAPL, divAmount, NOTES, NOW);

      assertThat(tx.metadata().get(TransactionMetadata.KEY_SYMBOL)).isEqualTo("AAPL");
      verify(account).deposit(eq(divAmount), contains("DIVIDEND"));
      verify(account, never()).applyPositionResult(any(), any());
    }
  }

  @Nested
  @DisplayName("Return of Capital (ROC)")
  class ROCTests {
    @Test
    @DisplayName("recordROC: success when quantity matches and position exists")
    void recordROC_Success() {
      // 1. Arrange
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN,
          new Money(new BigDecimal("1000.00"), USD), CREATION_DATE, NOW);

      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));
      when(account.getAccountId()).thenReturn(AccountId.newId());

      // 2. Act
      Transaction tx = service.recordReturnOfCapital(account, AAPL, TEN, HUNDRED_USD, NOTES, NOW);

      // 3. Assert
      assertThat(tx).isNotNull();
      assertThat(tx.transactionType()).isEqualTo(TransactionType.RETURN_OF_CAPITAL);
      verify(account).applyPositionResult(eq(AAPL), any());
      verify(account).deposit(eq(new Money(new BigDecimal("1000.00"), USD)), contains("RETURN OF CAPITAL: AAPL"));
    }

    @Test
    @DisplayName("recordROC: fail when no open position exists")
    void recordROC_ThrowsIfNoPosition() {
      when(account.getPosition(AAPL)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.recordReturnOfCapital(account, AAPL, TEN, HUNDRED_USD, NOTES, NOW))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("no open position");
    }

    @Test
    @DisplayName("recordROC: fail when quantity is partial (ROC must be for total holdings)")
    void recordROC_ThrowsOnPartialQuantity() {
      Position existingPos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN,
          new Money(new BigDecimal("1000.00"), USD), CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(existingPos));

      Quantity partialQty = new Quantity(new BigDecimal("5.00"));

      assertThatThrownBy(() -> service.recordReturnOfCapital(account, AAPL, partialQty, HUNDRED_USD, NOTES, NOW))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must match total held quantity");
    }

    @Test
    @DisplayName("recordReturnOfCapital: throw if quantity doesn't match held quantity")
    void recordROC_MismatchedQuantity_Throws() {
      mockPosition(AAPL, TEN, AssetType.STOCK);
      Quantity wrongQty = new Quantity(new BigDecimal("5"));

      assertThatThrownBy(() -> service.recordReturnOfCapital(account, AAPL, wrongQty, HUNDRED_USD, NOTES, NOW))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must match total held quantity");
    }
  }

  @Nested
  @DisplayName("Cash Accounting Operations")
  class CashAccountingTests {

    private static final Money HUNDRED_USD = new Money(new BigDecimal("100.00"), USD);

    @Test
    @DisplayName("recordDeposit: increase balance and verify positive cashDelta")
    void recordDeposit_Success() {
      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordDeposit(account, HUNDRED_USD, NOTES, NOW);

      // Verify the Account interaction
      verify(account).deposit(eq(HUNDRED_USD), eq("DEPOSIT"));

      // Verify the Transaction object returned
      assertThat(tx.transactionType()).isEqualTo(TransactionType.DEPOSIT);
      assertThat(tx.cashDelta()).isEqualTo(HUNDRED_USD);
      assertThat(tx.cashDelta().isPositive()).isTrue();
    }

    @Test
    @DisplayName("recordWithdrawal: decrease balance and verify negative cashDelta")
    void recordWithdrawal_Success() {
      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordWithdrawal(account, HUNDRED_USD, NOTES, NOW);

      // Verify Account interaction
      verify(account).withdraw(eq(HUNDRED_USD), eq("WITHDRAWAL"), eq(false));

      // Verify Transaction object returned - Withdrawal delta MUST be negative
      assertThat(tx.transactionType()).isEqualTo(TransactionType.WITHDRAWAL);
      assertThat(tx.cashDelta()).isEqualTo(HUNDRED_USD.negate());
      assertThat(tx.cashDelta().isNegative()).isTrue();
    }

    @Test
    @DisplayName("recordFee: withdraw from account and append amount to reason")
    void recordFee_Success() {
      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordFee(account, HUNDRED_USD, NOTES, NOW);

      // Service appends amount string to "FEE: "
      verify(account).withdraw(eq(HUNDRED_USD), contains("FEE: 100.00"), eq(false));

      assertThat(tx.transactionType()).isEqualTo(TransactionType.FEE);
      assertThat(tx.cashDelta()).isEqualTo(HUNDRED_USD.negate());
    }

    @Test
    @DisplayName("recordInterest: deposit with symbol metadata")
    void recordInterest_Success() {
      when(account.getAccountId()).thenReturn(AccountId.newId());

      Transaction tx = service.recordInterest(account, AAPL, HUNDRED_USD, NOTES, NOW);

      verify(account).deposit(eq(HUNDRED_USD), contains("INTEREST: AAPL"));

      assertThat(tx.transactionType()).isEqualTo(TransactionType.INTEREST);
      assertThat(tx.metadata().get(TransactionMetadata.KEY_SYMBOL)).isEqualTo("AAPL");
    }
  }

  @Nested
  @DisplayName("Unimplemented Transfers")
  class TransferTests {
    @Test
    @DisplayName("recordTransferIn: should throw UnsupportedOperationException")
    void recordTransferInThrows() {
      assertThatThrownBy(() -> service.recordTransferIn(account, Money.zero(USD), NOTES, NOW))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("recordTransferOut: should throw UnsupportedOperationException")
    void recordTransferOut_Throws() {
      assertThatThrownBy(() -> service.recordTransferOut(account, Money.zero(USD), NOTES, NOW))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("not yet implemented");
    }
  }

  @Nested
  @DisplayName("Replay Logic")
  class ReplayTests {
    @Test
    @DisplayName("Branch 1: execution is NULL (Short-circuit OR)")
    void branch1_ExecutionIsNull() {
      // Arrange: Force execution to be null even if type usually requires it
      Transaction mockTx = mock(Transaction.class);
      TransactionType mockType = mock(TransactionType.class);

      when(mockTx.isExcluded()).thenReturn(false);
      when(mockTx.transactionType()).thenReturn(mockType);

      // Even if affectsHoldings is true, if execution is null, it should return
      when(mockType.affectsHoldings()).thenReturn(true);
      when(mockTx.execution()).thenReturn(null);

      // Act
      service.replayTransaction(account, mockTx);

      // Assert: Verify it returned early before hitting the Position logic
      verify(account, never()).getPosition(any());
      verify(account, never()).ensurePosition(any(), any());
    }

    @Test
    @DisplayName("Branch 2: execution is NOT NULL but affectsHoldings is FALSE (Chained)")
    void branch2_DoesNotAffectHoldings() {
      // Arrange
      Transaction mockTx = mock(Transaction.class);
      TransactionType mockType = mock(TransactionType.class);

      when(mockTx.isExcluded()).thenReturn(false);
      when(mockTx.transactionType()).thenReturn(mockType);
      when(mockTx.execution()).thenReturn(mock(TradeExecution.class));

      // STUB CHAINING:
      // 1st call: returns TRUE (to pass the replayTransaction guard)
      // 2nd call: returns FALSE (to trigger the return in applyPositionEffect)
      when(mockType.affectsHoldings()).thenReturn(true, false);

      // Act
      service.replayTransaction(account, mockTx);

      // Assert:
      // If successful, the code passed the first guard but exited
      // before calling getPosition in the private method.
      verify(account, never()).getPosition(any());

      // Optional: Verify it was actually called twice to confirm the chain worked
      verify(mockType, times(2)).affectsHoldings();
    }

    @Test
    @DisplayName("Branch 3: execution is NOT NULL and affectsHoldings is TRUE (Logic Path)")
    void branch3_ProceedsToLogic() {
      // Arrange
      Transaction mockTx = mock(Transaction.class);
      TransactionType mockType = mock(TransactionType.class);
      TradeExecution mockExec = mock(TradeExecution.class);

      when(mockTx.isExcluded()).thenReturn(false);
      when(mockTx.transactionType()).thenReturn(mockType);
      when(mockTx.execution()).thenReturn(mockExec);
      when(mockTx.cashDelta()).thenReturn(Money.of(10, USD));
      when(mockTx.execution().quantity()).thenReturn(Quantity.of(10));
      when(mockTx.metadata()).thenReturn(TransactionMetadata.manual(AssetType.STOCK));

      // This combination passes the IF guard
      when(mockType.affectsHoldings()).thenReturn(true);
      when(mockExec.asset()).thenReturn(AAPL);

      // Mock the account to prevent NPE in the rest of the method
      when(account.getPosition(AAPL)).thenReturn(Optional.of(
          new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, Money.zero(USD), NOW, NOW)));

      // Act
      service.replayTransaction(account, mockTx);

      // Assert: Proves we passed the guard and reached the Account state logic
      verify(account).getPosition(AAPL);
      verify(account).applyPositionResult(eq(AAPL), any());
    }

    @Test
    @DisplayName("replayTransaction: successfully applies position effect to account")
    void replayTransaction_Success_CallsApplyPositionEffect() {
      // 1. Arrange
      // We use a BUY because it 'affectsHoldings'
      AccountId newId = AccountId.newId();
      Money price = new Money(new BigDecimal("100.00"), USD);
      Money delta = Money.of(-1000, "USD");
      Transaction tx = Transaction.builder()
          .transactionId(TransactionId.newId())
          .accountId(newId)
          .transactionType(TransactionType.BUY)
          .execution(new TradeExecution(AAPL, TEN, new Price(price)))
          .cashDelta(delta)
          .fees(List.of())
          .notes(NOTES)
          .metadata(TransactionMetadata.manual(AssetType.STOCK))
          .occurredAt(NOW)
          .build();

      // Setup account to allow the flow
      when(account.isInReplayMode()).thenReturn(false);
      // applyPositionEffect calls getPosition.
      // For a BUY, it first calls ensurePosition, then getPosition.
      Position mockPosition = new AcbPosition(AAPL, AssetType.STOCK, USD, Quantity.ZERO, Money.zero(USD), NOW, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(mockPosition));

      // 2. Act
      service.replayTransaction(account, tx);

      // 3. Assert
      // Verify the internal 'applyPositionEffect' logic was reached:
      // It should have ensured the position existed...
      verify(account).ensurePosition(AAPL, AssetType.STOCK);

      // ...and then applied the result of the TransactionApplier to the account
      verify(account).applyPositionResult(eq(AAPL), any(Position.class));

      // Double check: ReplayTransaction is "position-only", so NO cash should move
      verify(account, never()).withdraw(any(), anyString(), anyBoolean());
      verify(account, never()).deposit(any(), anyString());
    }

    @Test
    @DisplayName("applyPositionEffect: guard against null execution even if type affects holdings")
    void applyPositionEffect_GuardAgainstNullExecution() {
      // 1. Arrange
      // We mock the Transaction to bypass the record's constructor validation
      Transaction mockTx = mock(Transaction.class);
      when(mockTx.isExcluded()).thenReturn(false);
      when(mockTx.transactionType()).thenReturn(TransactionType.SPLIT);
      when(mockTx.execution()).thenReturn(null); // The specific condition we want to test

      when(account.isInReplayMode()).thenReturn(false);

      // 2. Act
      service.replayTransaction(account, mockTx);

      // 3. Assert
      // The guard 'if (tx.execution() == null)' should trigger and return early
      verify(account, never()).getPosition(any());
      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("applyPositionEffect: return early if type does not affect holdings")
    void applyPositionEffect_ReturnsEarlyForNonHoldingType() {
      // 1. Arrange
      Transaction interestTx = Transaction.builder()
          .transactionId(TransactionId.newId())
          .accountId(AccountId.newId())
          .transactionType(TransactionType.INTEREST) // affectsHoldings = false
          .execution(null)
          .cashDelta(new Money(BigDecimal.TEN, USD))
          .fees(List.of())
          .metadata(TransactionMetadata.manual(AssetType.CASH))
          .notes("Interest Income")
          .occurredAt(NOW)
          .build();

      // 2. Act
      // We call the private method logic by using a pathway that hits it.
      // Note: replayTransaction has its own guard, so we'd need to call
      // a method that doesn't block INTEREST, or mock the behavior.

      // In your current code, recordInterest doesn't call applyPositionEffect.
      // Only recordBuy, recordSell, recordROC, recordDRIP and Replays do.

      service.replayFullTransaction(account, List.of(interestTx));

      // 3. Assert
      // executeReplayStep calls applyPositionEffect for ALL transactions.
      // This proves applyPositionEffect correctly ignores INTEREST.
      verify(account, never()).getPosition(any());
    }

    @Test
    @DisplayName("executeReplayStep: case of NONE no cash effect (DRIP)")
    void executeReplayStep_CaseNone_NoCashEffect() {
      // 1. Arrange
      AccountId newId = AccountId.newId();
      Money price = new Money(new BigDecimal("100.00"), USD);
      // DRIP has impact NONE, so delta is effectively ignored by the cash switch
      Money delta = Money.zero(USD);

      Transaction tx = Transaction.builder()
          .transactionId(TransactionId.newId())
          .accountId(newId)
          .transactionType(TransactionType.DIVIDEND_REINVEST) // CashImpact.NONE
          .execution(new TradeExecution(AAPL, TEN, new Price(price)))
          .cashDelta(delta)
          .fees(List.of())
          .notes(NOTES)
          .metadata(TransactionMetadata.manual(AssetType.STOCK))
          .occurredAt(NOW)
          .build();

      // Position setup so applyPositionEffect doesn't throw
      Position initialPos = AcbPosition.empty(AAPL, AssetType.STOCK, USD);
      when(account.getPosition(eq(AAPL))).thenReturn(Optional.of(initialPos));

      // 2. Act
      service.replayFullTransaction(account, List.of(tx));

      // 3. Assert
      // Verify position logic WAS called (since DRIP affectsHoldings)
      verify(account).ensurePosition(AAPL, AssetType.STOCK);
      verify(account).applyPositionResult(eq(AAPL), any());

      // Verify CASE NONE logic: No cash was moved
      verify(account, never()).deposit(any(), anyString());
      verify(account, never()).withdraw(any(), anyString(), anyBoolean());

      // Verify the "Replay" lifecycle was managed
      verify(account).beginReplay();
      verify(account).endReplay();
    }

    @Test
    @DisplayName("replayTransaction: skip if transaction is excluded")
    void replayTransactionSkipsIfExcluded() {
      Transaction excludedTx = mock(Transaction.class);
      when(excludedTx.isExcluded()).thenReturn(true);

      service.replayTransaction(account, excludedTx);

      verify(account, never()).getPosition(any());
      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("replayTransaction: fail if tx does not affect holdings")
    void replayTransactionThrowsIfNoHoldingImpact() {
      // A Deposit does not affect holdings (it's cash-only)
      when(account.getAccountId()).thenReturn(AccountId.newId());
      Transaction depositTx = Transaction.builder()
          .transactionId(TransactionId.newId())
          .accountId(account.getAccountId())
          .transactionType(TransactionType.DEPOSIT)
          .cashDelta(Money.of("100", USD))
          .fees(List.of())
          .notes(NOTES)
          .occurredAt(CREATION_DATE)
          .relatedTransactionId(null)
          .metadata(TransactionMetadata.manual(AssetType.CASH))
          .build();

      when(account.isInReplayMode()).thenReturn(false);

      assertThatThrownBy(() -> service.replayTransaction(account, depositTx))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("position-only");
    }

    @Test
    @DisplayName("replayFullTransaction: skip excluded items in history")
    void replayFullSkipsExcludedItems() {
      Transaction activeTx = mock(Transaction.class);
      Transaction excludedTx = mock(Transaction.class);

      when(activeTx.isExcluded()).thenReturn(false);
      when(activeTx.transactionType()).thenReturn(TransactionType.BUY);
      when(excludedTx.isExcluded()).thenReturn(true);

      // Setup for executeReplayStep
      when(activeTx.cashDelta()).thenReturn(new Money(BigDecimal.ONE, USD));

      service.replayFullTransaction(account, List.of(activeTx, excludedTx));

      verify(activeTx, atLeastOnce()).transactionType();
      verify(excludedTx, never()).transactionType();
    }

    @Test
    @DisplayName("replayFullTransaction: propagates exception and ensures endReplay is called")
    void replayFullPropagatesException() {
      Transaction tx = mock(Transaction.class);
      when(tx.isExcluded()).thenReturn(false);
      when(tx.transactionType()).thenThrow(new RuntimeException("Crash during replay"));

      assertThatThrownBy(() -> service.replayFullTransaction(account, List.of(tx)))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Crash during replay");

      // Critical: endReplay MUST be called even on failure to restore account state
      verify(account).beginReplay();
      verify(account).endReplay();
    }

    @Test
    @DisplayName("replayTransaction: throw if account is already in replay mode")
    void replayTransactionAlreadyReplaying_Throws() {
      when(account.isInReplayMode()).thenReturn(true);
      Transaction tx = Transaction.builder().transactionId(TransactionId.newId()).accountId(AccountId.newId())
          .transactionType(TransactionType.BUY)
          .execution(new TradeExecution(AAPL, Quantity.of(10), Price.of("135", USD)))
          .cashDelta(Money.of(-1350, "USD")).fees(List.of()).notes("test note")
          .occurredAt(Instant.now()).metadata(TransactionMetadata.manual(AssetType.STOCK)).build();

      assertThatThrownBy(() -> service.replayTransaction(account, tx))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("replayFullTransaction: ensure replay mode does nothing when null history")
    void replayFullDoesNotWhenHistoryisNull() {
      service.replayFullTransaction(account, null);

      verify(account).beginReplay();
      verify(account).endReplay();
    }

    @Test
    @DisplayName("replayFullTransaction: ensure replay mode toggled even on empty list")
    void replayFullTogglesMode() {
      service.replayFullTransaction(account, new ArrayList<>());

      verify(account).beginReplay();
      verify(account).endReplay();
    }

    @Test
    @DisplayName("executeReplayStep: use allowNegative=true for withdrawals")
    void replayStep_Withdrawal_AllowsNegative() {
      Transaction tx = Transaction.builder()
          .transactionId(TransactionId.newId())
          .accountId(AccountId.newId())
          .transactionType(TransactionType.WITHDRAWAL)
          .cashDelta(new Money(new BigDecimal("-100"), USD))
          .fees(List.of())
          .notes(NOTES)
          .occurredAt(CREATION_DATE)
          .metadata(TransactionMetadata.manual(AssetType.CASH))
          .build();

      // This is internal, but we test it via the public replayFullTransaction
      service.replayFullTransaction(account, List.of(tx));

      verify(account).withdraw(eq(new Money(new BigDecimal("100"), USD)), anyString(), eq(true));
    }
  }

  @Nested
  @DisplayName("Validation and Edge Cases")
  class ValidationTests {
    @Test
    @DisplayName("validateIsActive: throw AccountClosedException when closed")
    void closedAccount_ThrowsException() {
      when(account.isActive()).thenReturn(false);

      assertThatThrownBy(() -> service.recordDeposit(account, new Money(BigDecimal.ONE, USD), NOTES, NOW))
          .isInstanceOf(AccountClosedException.class);
    }

    @Test
    @DisplayName("validateTradeInputs: throw NPE on null symbol")
    void nullInputs_ThrowNPE() {
      assertThatThrownBy(() -> service.recordBuy(account, null, AssetType.STOCK, TEN, HUNDRED_USD, null, NOTES, NOW))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("validateTransactionDate: throw if date is before account creation")
    void dateBeforeCreation_Throws() {
      Instant earlyDate = CREATION_DATE.minusSeconds(3600);
      // Note: ValidationUtils is likely static and mocked via Mockito.mockStatic if
      // needed,
      // but here we assume it works as written in the service.

      assertThatThrownBy(() -> service.recordDeposit(account, new Money(BigDecimal.ONE, USD), NOTES, earlyDate))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}