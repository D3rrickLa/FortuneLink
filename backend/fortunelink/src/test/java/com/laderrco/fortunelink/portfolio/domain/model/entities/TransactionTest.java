package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee.FeeMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Transaction Entity Tests")
public class TransactionTest {
  private static final AssetSymbol AAPL = new AssetSymbol("AAPL");
  private static final Currency USD = Currency.USD;
  private static final Price P135 = Price.of("135", USD);
  private static final Quantity QTY10 = Quantity.of(10);

  /**
   * Standard BUY execution: 10 × $135 = $1,350 gross
   */
  private static TradeExecution buyExecution() {
    return new TradeExecution(AAPL, QTY10, P135);
  }

  /**
   * Pre-wired BUY builder; override only what the test cares about.
   */
  private static Transaction.TransactionBuilder validBuy() {
    return Transaction.builder().transactionId(TransactionId.newId()).accountId(AccountId.newId())
        .transactionType(TransactionType.BUY).execution(buyExecution())
        .cashDelta(Money.of(-1350, "USD")).fees(List.of()).notes("test note")
        .occurredAt(Instant.now()).metadata(TransactionMetadata.manual(AssetType.STOCK));
  }

  @Nested
  @DisplayName("Construction invariants")
  class ConstructionTests {
    static Stream<Arguments> buyAndSellDeltas() {
      return Stream.of(Arguments.of(TransactionType.BUY, -1350),
          Arguments.of(TransactionType.SELL, 1350));
    }

    @ParameterizedTest
    @MethodSource("buyAndSellDeltas")
    @DisplayName("BUY/SELL: constructs successfully with correct signed cash delta")
    void buyAndSellWithCorrectDelta(TransactionType type, int delta) {
      var tx = validBuy().transactionType(type).cashDelta(Money.of(delta, "USD")).build();
      assertEquals(type, tx.transactionType());
    }

    @Test
    @DisplayName("DIVIDEND_REINVEST: accepts zero cash delta with execution present")
    void dividendReinvestAcceptsZeroDelta() {
      var tx = validBuy().transactionType(TransactionType.DIVIDEND_REINVEST)
          .cashDelta(Money.zero(USD)).build();
      assertTrue(tx.cashDelta().isZero());
    }

    @Test
    @DisplayName("DIVIDEND: accepts non-execution cash inflow with empty fee list")
    void dividendAcceptsNoExecution() {
      var tx = validBuy().transactionType(TransactionType.DIVIDEND).execution(null)
          .cashDelta(Money.of(1350, "USD")).build();
      assertTrue(tx.fees().isEmpty());
    }

    @Test
    @DisplayName("BUY: throws when execution is absent")
    void buyThrowsWhenExecutionNull() {
      var ex = assertThrows(IllegalArgumentException.class,
          () -> validBuy().execution(null).build());
      assertThat(ex.getMessage()).contains("requires execution");
    }

    @Test
    @DisplayName("DEPOSIT: throws when execution is present")
    void depositThrowsWithExecution() {
      var ex = assertThrows(IllegalArgumentException.class,
          () -> validBuy().transactionType(TransactionType.DEPOSIT).cashDelta(Money.of(1350, "USD"))
              .build());
      assertThat(ex.getMessage()).contains("cannot have execution");
    }

    @Test
    @DisplayName("BUY: throws when an unrelated split ratio is attached")
    void buyThrowsWithSplitRatio() {
      var split = new Ratio(12, 1);
      var ex = assertThrows(IllegalArgumentException.class, () -> validBuy().split(split).build());
      assertThat(ex.getMessage()).contains("cannot have split details");
      
      assertThat(split.multiplier()).isEqualByComparingTo(BigDecimal.valueOf(12)
          .setScale(Precision.DIVISION.getDecimalPlaces(), Rounding.DIVISION.getMode()));
    }

    @Test
    @DisplayName("SPLIT: throws when split ratio is null")
    void splitThrowsWhenRatioNull() {
      var ex = assertThrows(IllegalArgumentException.class,
          () -> validBuy().transactionType(TransactionType.SPLIT).split(null).build());
      assertThat(ex.getMessage()).contains("requires split details");
    }

    @Test
    @DisplayName("DIVIDEND_REINVEST: throws when cash delta is non-zero")
    void dividendReinvestThrowsOnNonZeroDelta() {
      var ex = assertThrows(IllegalArgumentException.class,
          () -> validBuy().transactionType(TransactionType.DIVIDEND_REINVEST)
              .cashDelta(Money.of(1, "USD")).build());
      assertThat(ex.getMessage()).contains("cannot affect cash");
    }

    @Test
    @DisplayName("non-trade types: throws when a fee list is attached")
    void nonTradeTypeThrowsWithFees() {
      var fee = new Fee(FeeType.ACCOUNT_MAINTENANCE, Money.of(5, "USD"), Money.zero(USD),
          ExchangeRate.identity(USD, Instant.now()), Instant.now(), new FeeMetadata(Map.of()));

      var ex = assertThrows(IllegalArgumentException.class,
          () -> validBuy().transactionType(TransactionType.TRANSFER_OUT).execution(null)
              .cashDelta(Money.zero(USD)).fees(List.of(fee)).build());
      assertThat(ex.getMessage()).contains("cannot have fees");
    }
  }

  @Nested
  @DisplayName("Builder test")
  public class BuilderTest {
    @Test
    @DisplayName("builder: builds transaction")
    void builderBuilds() {
      Fee fee = Fee.of(FeeType.ACCOUNT_MAINTENANCE, Money.of(1, USD), Instant.now());
      Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(AccountId.newId()).transactionType(TransactionType.BUY)
          .execution(buyExecution()).cashDelta(Money.of(-1351, "USD")).fee(fee).notes("test note")
          .relatedTransactionId(TransactionId.newId()).occurredAt(Instant.now())
          .metadata(TransactionMetadata.manual(AssetType.STOCK)).build();

      assertNotNull(tx);
      assertEquals(fee, tx.fees().getFirst());
    }

  }

  @Nested
  @DisplayName("Cash delta consistency (validateTradeConsistency)")
  class CashDeltaConsistencyTests {
    @Test
    @DisplayName("BUY: throws when cash delta does not match gross + fees")
    void buyThrowsOnDeltaMismatch() {
      
      var ex = assertThrows(IllegalArgumentException.class,
          () -> validBuy().cashDelta(Money.of(1350, "USD")).build());
      assertThat(ex.getMessage()).contains("Cash delta mismatch");
    }

    @Test
    @DisplayName("SPLIT: cash delta validation routes to NONE branch (expects zero)")
    void splitCashDeltaMustBeZero() {
      
      
      
      var ex = assertThrows(IllegalArgumentException.class,
          () -> validBuy().transactionType(TransactionType.SPLIT).cashDelta(Money.of(1350, "USD"))
              .build());
      assertThat(ex.getMessage()).contains("requires");
    }

    @Test
    @DisplayName("Transaction: throw exception if cashDelta sign is wrong for BUY")
    void transactionThrowsOnWrongSign() {
      assertThatThrownBy(() -> Transaction.builder().transactionType(TransactionType.BUY)
          .cashDelta(Money.of(1000, USD)) 
          .execution(new TradeExecution(AAPL, Quantity.of(10), Price.of("100", USD)))
          .build()).isInstanceOf(DomainArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Fee aggregation (totalFeesInAccountCurrency)")
  class FeeAggregationTests {
    @Test
    @DisplayName("multi-currency fees sum to account currency via accountAmount field")
    void multiCurrencyFeesTotalInAccountCurrency() {
      var usdFee = new Fee(FeeType.BROKERAGE, Money.of(5, "USD"), Money.of(5, "USD"),
          ExchangeRate.identity(USD, Instant.now()), Instant.now(), new FeeMetadata(Map.of()));

      var cadFee = new Fee(FeeType.BROKERAGE, Money.of(5, "CAD"), Money.of(3.25, "USD"),
          
          new ExchangeRate(Currency.CAD, USD, BigDecimal.valueOf(0.65), Instant.now()),
          Instant.now(), new FeeMetadata(Map.of()));

      var tx = validBuy().cashDelta(Money.of(-1358.25, "USD")) 
          .fees(List.of(usdFee, cadFee)).metadata(TransactionMetadata.manual(AssetType.STOCK))
          .build();

      var total = tx.totalFeesInAccountCurrency();
      assertAll(() -> assertEquals(Money.of(8.25, "USD"), total),
          () -> assertEquals("USD", total.currency().getCode()),
          () -> assertEquals(2, total.currency().getDefaultFractionDigits()));
    }
  }

  @Nested
  @DisplayName("Exclude and restore lifecycle")
  class ExcludeRestoreTests {
    private Transaction transaction;

    @BeforeEach
    void setUp() {
      transaction = validBuy().build();
    }

    @Test
    @DisplayName("exclude: stamps userId and reason onto exclusion record")
    void excludeSetsExclusionRecord() {
      var userId = UserId.random();
      var excluded = transaction.markAsExcluded(userId, "testing");

      assertAll(() -> assertTrue(excluded.isExcluded()),
          () -> assertEquals(userId, excluded.metadata().exclusion().by()));
      assertFalse(transaction.isExcluded(), "original must be immutable");
    }

    @Test
    @DisplayName("restore: clears exclusion record from previously excluded transaction")
    void restoreClearsExclusionRecord() {
      var excluded = transaction.markAsExcluded(UserId.random(), "testing");
      var restored = excluded.restore();

      assertFalse(restored.isExcluded());
      assertNull(restored.metadata().exclusion());
    }

    @Test
    @DisplayName("exclude: throws when transaction is already excluded")
    void excludeThrowsWhenAlreadyExcluded() {
      var excluded = transaction.markAsExcluded(UserId.random(), "first");
      var ex = assertThrows(IllegalStateException.class,
          () -> excluded.markAsExcluded(UserId.random(), "second"));
      assertThat(ex.getMessage()).contains("already exclude");
    }

    @Test
    @DisplayName("restore: throws when transaction has not been excluded")
    void restoreThrowsWhenNotExcluded() {
      var ex = assertThrows(IllegalStateException.class, () -> transaction.restore());
      assertThat(ex.getMessage()).contains("not excluded");
    }
  }

  @Nested
  @DisplayName("TradeExecution")
  class TradeExecutionTests {
    @Test
    @DisplayName("grossValue: price × quantity regardless of buy/sell direction")
    void grossValueIsSignIndependent() {
      var execution = new TradeExecution(AAPL, QTY10, P135);
      assertEquals(Money.of(1350.00, "USD"), execution.grossValue());
    }

    @Test
    @DisplayName("construction: throws on null symbol, zero quantity, or negative price")
    void throwsOnInvalidInputs() {
      assertAll(() -> assertThrows(DomainArgumentException.class,
              () -> new TradeExecution(null, QTY10, P135)),
          () -> assertThrows(IllegalArgumentException.class,
              () -> new TradeExecution(AAPL, QTY10, new Price(Money.of(-1.00, "USD")))),
          () -> assertThrows(IllegalArgumentException.class,
              () -> new TradeExecution(AAPL, new Quantity(BigDecimal.ZERO), P135)));
    }
  }
}
