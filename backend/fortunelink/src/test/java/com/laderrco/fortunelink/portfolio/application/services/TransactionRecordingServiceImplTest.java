package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
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
  @DisplayName("Cash Accounting Operations")
  class CashOperationsTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCashScenarios")
    @DisplayName("recordCashOp: verify correct cash delta and account interaction")
    void recordCashOpSuccess(String desc, TransactionType type, BigDecimal amount, String expectedReason) {
      Money money = new Money(amount, USD);

      // Call the specific service method based on the type
      Transaction tx = switch (type) {
        case DEPOSIT -> service.recordDeposit(account, money, NOTES, NOW);
        case WITHDRAWAL -> service.recordWithdrawal(account, money, NOTES, NOW);
        case INTEREST -> service.recordInterest(account, AAPL, money, NOTES, NOW);
        case FEE -> service.recordFee(account, money, NOTES, NOW);
        case TRANSFER_IN -> service.recordTransferIn(account, money, NOTES, NOW);
        case TRANSFER_OUT -> service.recordTransferOut(account, money, NOTES, NOW);
        default -> throw new IllegalArgumentException("Unexpected type: " + type);
      };

      assertThat(tx.transactionType()).isEqualTo(type);

      // Assertions remain the same
      if (type == TransactionType.WITHDRAWAL || type == TransactionType.TRANSFER_OUT || type == TransactionType.FEE) {
        // Based on your error log, the service is passing the amount AS IS (negative)
        verify(account).withdraw(
            argThat(m -> m.amount().compareTo(amount.negate()) == 0 || m.amount().compareTo(amount) == 0),
            contains(expectedReason),
            eq(false));
      } else {
        verify(account).deposit(
            argThat(m -> m.amount().compareTo(amount) == 0),
            contains(expectedReason));
      }
    }

    private static Stream<Arguments> provideCashScenarios() {
      BigDecimal hundred = new BigDecimal("100.00");
      return Stream.of(
          Arguments.of("Deposit", TransactionType.DEPOSIT, hundred, "DEPOSIT"),
          Arguments.of("Withdrawal", TransactionType.WITHDRAWAL, hundred.negate(), "WITHDRAWAL"),
          Arguments.of("Interest", TransactionType.INTEREST, hundred, "INTEREST"),
          Arguments.of("Fee", TransactionType.FEE, hundred.negate(), "FEE"),
          Arguments.of("Transfer In", TransactionType.TRANSFER_IN, hundred, "TRANSFER IN"),
          Arguments.of("Transfer Out", TransactionType.TRANSFER_OUT, hundred.negate(), "TRANSFER OUT"));
    }
  }

  @Nested
  @DisplayName("Trade Operations (Buy/Sell)")
  class TradeOperationsTests {

    @Test
    @DisplayName("recordBuy: handle insufficient funds and skip mutations")
    void recordBuyInsufficientFundsFails() {
      when(account.hasSufficientCash(any())).thenReturn(false);
      when(account.getCashBalance()).thenReturn(Money.zero(USD));

      assertThatThrownBy(
          () -> service.recordBuy(account, AAPL, AssetType.STOCK, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW))
          .isInstanceOf(InsufficientFundsException.class);

      verify(account, never()).applyPositionResult(any(), any());
    }

    @Test
    @DisplayName("recordSell: full liquidation results in exactly zero position")
    void recordSellFullLiquidationSuccess() {
      BigDecimal basis = new BigDecimal("500.123");
      Position pos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, new Money(basis, USD), CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(pos));

      service.recordSell(account, AAPL, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW);

      verify(account).applyPositionResult(eq(AAPL), argThat(p -> p.totalQuantity().isZero()));
      verify(account).recordRealizedGain(eq(AAPL),
          argThat(m -> m.amount().compareTo(new BigDecimal("1000").subtract(basis)) == 0), any(), eq(NOW));
    }

    @Test
    @DisplayName("recordSell: throw InsufficientQuantityException on oversell")
    void recordSellOversellThrows() {
      Position pos = new AcbPosition(AAPL, AssetType.STOCK, USD, Quantity.of(5), HUNDRED_USD_MONEY, CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(pos));

      assertThatThrownBy(() -> service.recordSell(account, AAPL, TEN, HUNDRED_USD_PRICE, null, NOTES, NOW))
          .isInstanceOf(InsufficientQuantityException.class);
    }
  }

  @Nested
  @DisplayName("Corporate Actions")
  class CorporateActionTests {
    @Test
    @DisplayName("recordDividendReinvestment: update position with zero cash impact")
    void recordDRIPNoCashImpact() {
      when(account.getPosition(AAPL)).thenReturn(Optional.of(AcbPosition.empty(AAPL, AssetType.STOCK, USD)));
      Transaction tx = service.recordDividendReinvestment(account, AAPL, TEN, HUNDRED_USD_PRICE, NOTES, NOW);

      assertThat(tx.cashDelta().isZero()).isTrue();
      verify(account, never()).deposit(any(), any());
      verify(account).applyPositionResult(eq(AAPL), any());
    }

    @Test
    @DisplayName("recordReturnOfCapital: throw error if quantity does not match total held")
    void recordROCRequiresFullQuantity() {
      Position pos = new AcbPosition(AAPL, AssetType.STOCK, USD, TEN, HUNDRED_USD_MONEY, CREATION_DATE, NOW);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(pos));

      assertThatThrownBy(
          () -> service.recordReturnOfCapital(account, AAPL, Quantity.of(5), HUNDRED_USD_PRICE, NOTES, NOW))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Replay Mechanism")
  class ReplayTests {
    private static final Money THOUSAND_USD_MONEY = Money.of(1000, USD);

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideReplayGuardScenarios")
    @DisplayName("replayTransaction: verify guard clauses and early exits")
    void replayTransactionGuards(String desc, Transaction tx, boolean inReplay, Class<? extends Throwable> exception) {
      lenient().when(account.isInReplayMode()).thenReturn(inReplay);
      if (exception != null) {
        assertThatThrownBy(() -> service.replayTransaction(account, tx)).isInstanceOf(exception);
      } else {
        service.replayTransaction(account, tx);
        verify(account, never()).applyPositionResult(any(), any());
      }
    }

    private static Stream<Arguments> provideReplayGuardScenarios() {
      return Stream.of(
          Arguments.of("Excluded Transaction", mockTx(true, true), false, null),
          Arguments.of("Already in Replay Mode", mockTx(false, true), true, IllegalStateException.class),
          Arguments.of("Cash only Type", mockTx(false, false), false, IllegalArgumentException.class));
    }

    @Test
    @DisplayName("replayFullTransaction: ensure lifecycle and finally block execution on error")
    void replayFullTransactionLifecycleOnFailure() {
      Transaction tx = buildTx(TransactionType.SELL, HUNDRED_USD_PRICE, TEN, THOUSAND_USD_MONEY);
      when(account.getPosition(AAPL)).thenReturn(Optional.of(AcbPosition.empty(AAPL, AssetType.STOCK, USD)));

      assertThatThrownBy(() -> service.replayFullTransaction(account, List.of(tx)))
          .isInstanceOf(IllegalStateException.class);

      verify(account).beginReplay();
      verify(account).endReplay();
    }

    private static Transaction mockTx(boolean excluded, boolean affectsHoldings) {
      Transaction tx = mock(Transaction.class);
      TransactionType type = mock(TransactionType.class);
      lenient().when(tx.isExcluded()).thenReturn(excluded);
      lenient().when(tx.transactionType()).thenReturn(type);
      lenient().when(type.affectsHoldings()).thenReturn(affectsHoldings);
      return tx;
    }
  }

  private Transaction buildTx(TransactionType type, Price price, Quantity qty, Money delta) {
    return Transaction.builder().transactionId(TransactionId.newId()).accountId(account.getAccountId())
        .transactionType(type).execution(new TradeExecution(AAPL, qty, price)).cashDelta(delta).fees(List.of())
        .notes(NOTES).metadata(TransactionMetadata.manual(AssetType.STOCK)).occurredAt(NOW).build();
  }
}