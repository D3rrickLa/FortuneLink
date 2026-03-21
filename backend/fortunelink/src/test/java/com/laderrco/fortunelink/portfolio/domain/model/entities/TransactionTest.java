package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

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
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata.ExclusionRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
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

public class TransactionTest {

  @Nested
  @DisplayName("Constructor tests")
  public class ConstructorTests {

    static Stream<Arguments> validTransactionProvider() {
      return Stream.of(Arguments.of(TransactionType.BUY, -1350), // Buying: Cash goes OUT (negative)
          Arguments.of(TransactionType.SELL, 1350) // Selling: Cash comes IN (positive)
      );
    }

    @ParameterizedTest
    @MethodSource("validTransactionProvider")
    void testConstructor_Success(TransactionType type, int delta) {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

      Ratio split = null;
      Money cashDelta = Money.of(delta, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      Transaction transaction = new Transaction(transactionId, accountId, type, execution, split,
          cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata);
      assertAll(() -> assertEquals(transactionId, transaction.transactionId()));
    }

    @Test
    void testConstructor_Success_ExpectedCashDeltaFiresNONECase() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.DIVIDEND_REINVEST;
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

      Ratio split = null;
      Money cashDelta = Money.of(0, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      Transaction transaction = new Transaction(transactionId, accountId, transactionType,
          execution, split, cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata);

      assertAll(() -> assertEquals(transactionId, transaction.transactionId()));
    }

    @Test
    void testConstructor_Success_WhenDividend_PassesWithEmptyFee() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.DIVIDEND;
      TradeExecution execution = null;
      Ratio split = null;
      Money cashDelta = Money.of(1350, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      Transaction transaction = new Transaction(transactionId, accountId, transactionType,
          execution, split, cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata);
      assertAll(() -> assertEquals(transactionId, transaction.transactionId()),
          () -> assertTrue(transaction.fees().isEmpty()));
    }

    @Test
    void testConstructor_Failure_WhenExecutionIsNullAndRequireExecutionIsTrue() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.BUY;
      TradeExecution execution = null;
      Ratio split = null;
      Money cashDelta = Money.of(1350, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> new Transaction(transactionId, accountId, transactionType, execution, split,
              cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

      assertTrue(ex.getMessage().contains("requires execution"));
    }

    @Test
    void testConstructor_Failure_WhenExecutionIsNonNullAndTypeIsNotValid() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.DEPOSIT;
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

      Ratio split = null;
      Money cashDelta = Money.of(1350, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> new Transaction(transactionId, accountId, transactionType, execution, split,
              cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

      assertTrue(ex.getMessage().contains("cannot have execution"));
    }

    @Test
    void testConstructor_Failure_WhenExecutionsplitIsNonNullAndIsNotsplit() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.BUY;
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

      Ratio split = new Ratio(12, 1);
      Money cashDelta = Money.of(1350, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> new Transaction(transactionId, accountId, transactionType, execution, split,
              cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

      assertTrue(ex.getMessage().contains("cannot have split details"));
      assertThat(split.multiplier()).isEqualTo(BigDecimal.valueOf(12)
          .setScale(Precision.DIVISION.getDecimalPlaces(), Rounding.DIVISION.getMode()));
    }

    @Test
    void testConstructor_Failure_RequiresSplitButsplitIsNull() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.SPLIT;
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

      Ratio split = null;
      Money cashDelta = Money.of(1350, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> new Transaction(transactionId, accountId, transactionType, execution, split,
              cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

      assertTrue(ex.getMessage().contains("requires split details"));
    }

    @Test
    void testConstructor_Failure_CashImpactIsNoneButDeltaIsFalse() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.DIVIDEND_REINVEST;
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"), Quantity.of(10),
          new Price(Money.of("20", Currency.USD)));
      Ratio split = null;
      Money cashDelta = Money.of(1, "USD"); // Invalid: Non-execution types cannot affect cash
      List<Fee> fees = List.of();
      String notes = "Testing cash validation";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> new Transaction(transactionId, accountId, transactionType, execution, split,
              cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

      assertTrue(ex.getMessage().contains("cannot affect cash"));
    }

    @Test
    void testConstructor_Failure_WhenNonExecutionHasFees() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.TRANSFER_OUT;
      TradeExecution execution = null;
      Ratio split = null;
      Money cashDelta = Money.zero("USD");
      List<Fee> fees = List.of(new Fee(FeeType.ACCOUNT_MAINTENANCE, Money.of(5, "USD"), cashDelta,
          ExchangeRate.identity(Currency.USD, Instant.now()), Instant.now(),
          new FeeMetadata(Map.of())));
      String notes = "Testing fee validation";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> new Transaction(transactionId, accountId, transactionType, execution, split,
              cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

      assertTrue(ex.getMessage().contains("cannot have fees"));
    }

    @Nested
    @DisplayName("validateTradeConsistencyTests")
    public class ValidateTradeConsistencyTests {
      @Test
      void testValidateTradeConsistency_Failure_WhenGrossValueNotBuy() {
        TransactionId transactionId = TransactionId.newId();
        AccountId accountId = AccountId.newId();
        TransactionType transactionType = TransactionType.BUY;
        TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
            new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

        Ratio split = null;
        Money cashDelta = Money.of(1350, "USD");
        List<Fee> fees = List.of();
        String notes = "Some notes";
        Instant occurredAt = Instant.now();
        TransactionId relatedTransactionId = null;
        TransactionMetadata metadata = mock(TransactionMetadata.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new Transaction(transactionId, accountId, transactionType, execution, split,
                cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

        assertTrue(ex.getMessage().contains("Cash delta mismatch"));
      }

      @Test
      void testValidateTradeConsistency_Failure_WhenExpectedCashDeltaDefaultPath() {
        TransactionId transactionId = TransactionId.newId();
        AccountId accountId = AccountId.newId();
        TransactionType transactionType = TransactionType.SPLIT;
        TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
            new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

        Ratio split = null;
        Money cashDelta = Money.of(1350, "USD");
        List<Fee> fees = List.of();
        String notes = "Some notes";
        Instant occurredAt = Instant.now();
        TransactionId relatedTransactionId = null;
        TransactionMetadata metadata = mock(TransactionMetadata.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new Transaction(transactionId, accountId, transactionType, execution, split,
                cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata));

        assertTrue(ex.getMessage().contains("requires"));
      }
    }
  }

  @Nested
  @DisplayName("TotalFeesInAccountCurrency tests")
  public class TotalFeesInAccountCurrencyTest {
    @Test
    void test_TotalFeesInAccountCurrency_Success() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.BUY;
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

      Ratio split = null;
      Money cashDelta = Money.of(-1358.25, "USD");
      List<Fee> fees = List.of(new Fee(FeeType.BROKERAGE, Money.of(5, "USD"), Money.of(5, "USD"),
              ExchangeRate.identity(Currency.USD, Instant.now()), Instant.now(),
              new FeeMetadata(Map.of())),
          new Fee(FeeType.BROKERAGE, Money.of(5, "CAD"), Money.of(3.25, "USD"),
              new ExchangeRate(Currency.CAD, Currency.USD, BigDecimal.valueOf(1.35), Instant.now()),
              Instant.now(), new FeeMetadata(Map.of()))

      );
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionId relatedTransactionId = null;
      TransactionMetadata metadata = TransactionMetadata.manual(AssetType.STOCK);

      Transaction transaction = new Transaction(transactionId, accountId, transactionType,
          execution, split, cashDelta, fees, notes, occurredAt, relatedTransactionId, metadata);
      assertAll(() -> assertEquals(Money.of(8.25, "USD"), transaction.totalFeesInAccountCurrency()),
          () -> assertEquals(Currency.USD.getCode(),
              transaction.totalFeesInAccountCurrency().currency().getCode()), () -> assertEquals(2,
              transaction.totalFeesInAccountCurrency().currency().getDefaultFractionDigits()));
    }

  }

  @Nested
  @DisplayName("Restore and Exclude methods")
  public class TransactionDeletionTests {
    private Transaction transaction;

    @BeforeEach
    void setUp() {
      TransactionId transactionId = TransactionId.newId();
      AccountId accountId = AccountId.newId();
      TransactionType transactionType = TransactionType.BUY;
      TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(BigDecimal.TEN), new Price(Money.of(135, "USD")));

      Ratio split = null;
      Money cashDelta = Money.of(-1350, "USD");
      List<Fee> fees = List.of();
      String notes = "Some notes";
      Instant occurredAt = Instant.now();
      TransactionMetadata metadata = TransactionMetadata.manual(AssetType.STOCK);

      transaction = new Transaction(transactionId, accountId, transactionType, execution, split,
          cashDelta, fees, notes, occurredAt, null, metadata);
    }

    @Test
    void testMarkAssetExcluded_Success_MarksTransactionAsExcluded() {
      UserId userId = UserId.random();

      assertFalse(transaction.isExcluded());

      Transaction excludedTransaction = transaction.markAsExcluded(userId, "For testing");

      assertTrue(excludedTransaction.metadata().isExcluded());
      assertEquals(userId, excludedTransaction.metadata().exclusion().by());
      assertTrue(excludedTransaction.isExcluded());
    }

    @Test
    void testRestore_Success_MarksTransactionAsRestored() {
      UserId userId = UserId.random();
      Transaction excludedTransaction = transaction.markAsExcluded(userId, "For testing");
      assertTrue(excludedTransaction.isExcluded());
      assertEquals(userId, excludedTransaction.metadata().exclusion().by());

      Transaction restored = excludedTransaction.restore();
      assertFalse(restored.metadata().isExcluded());
      assertNull(restored.metadata().exclusion());
    }

    @Test
    void testMarketAssetExcluded_Failure_AlreadyExcluded() {
      UserId userId = UserId.random();

      assertFalse(transaction.isExcluded());

      Transaction excludedTransaction = transaction.markAsExcluded(userId, "For testing");

      IllegalStateException stateException = assertThrows(IllegalStateException.class,
          () -> excludedTransaction.markAsExcluded(UserId.random(), "reason 2"));

      assertTrue(stateException.getMessage().contains("already exclude"));
      assertFalse(transaction.isExcluded());

    }

    @Test
    void testRestore_Failure_TransactionNotYetExcluded() {

      IllegalStateException stateException = assertThrows(IllegalStateException.class,
          () -> transaction.restore());

      assertTrue(stateException.getMessage().contains("Transaction is not excluded"));
      assertFalse(transaction.isExcluded());

    }
  }

  @Nested
  class TradeExecutionTest {

    @Test
    @DisplayName("Should calculate gross value correctly for both Buy and Sell")
    void testGrossValue() {
      Price price = new Price(Money.of(150.00, "USD"));

      // Test Buy (Positive Quantity)
      var buy = new Transaction.TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(new BigDecimal("10")), price);
      assertEquals(Money.of(1500.00, "USD"), buy.grossValue());

      // Test Sell (Negative Quantity) - Gross value should stay positive/absolute
      var sell = new Transaction.TradeExecution(new AssetSymbol("AAPL"),
          new Quantity(new BigDecimal("10")), price);
      assertEquals(Money.of(1500.00, "USD"), sell.grossValue());
    }

    @Test
    @DisplayName("Should throw exception for invalid inputs")
    void testInvariants() {
      AssetSymbol symbol = new AssetSymbol("MSFT");
      Quantity qty = new Quantity(new BigDecimal("10"));
      Price price = new Price(Money.of(100.00, "USD"));

      assertThrows(DomainArgumentException.class,
          () -> new Transaction.TradeExecution(null, qty, price));
      assertThrows(IllegalArgumentException.class,
          () -> new Transaction.TradeExecution(symbol, qty, new Price(Money.of(-1.00, "USD"))));
      assertThrows(IllegalArgumentException.class,
          () -> new Transaction.TradeExecution(symbol, new Quantity(new BigDecimal("0")), price));
    }

  }

  @Nested
  class TransactionMetadataTest {
    @Test
    void testNullSafetyAndDefaults() {
      // Test that null map becomes empty map and null source becomes UNKNOWN
      var meta = new TransactionMetadata(AssetType.STOCK, null, null, null);

      assertEquals("UNKNOWN", meta.source());
      assertNotNull(meta.additionalData());
      assertTrue(meta.isEmpty());
    }

    @Test
    void testNullSafetyAndDefaultsExcluded() {
      // Test that null map becomes empty map and null source becomes UNKNOWN
      TransactionMetadata.ExclusionRecord exclusionRecord = new ExclusionRecord(Instant.now(),
          UserId.random(), "testing");
      var meta = new TransactionMetadata(AssetType.STOCK, null, exclusionRecord, null);

      assertEquals("UNKNOWN", meta.source());
      assertNotNull(meta.additionalData());
      assertTrue(meta.isEmpty());
    }

    @Test
    void testImmutabilityOfAdditionalData() {
      Map<String, String> originalData = new HashMap<>();
      originalData.put("key", "value");

      var meta = new TransactionMetadata(AssetType.STOCK, "SOURCE", null, originalData);

      // Try to modify original map
      originalData.put("key", "changed");

      // Record should remain unchanged because of Map.copyOf()
      assertEquals("value", meta.get("key"));
      assertEquals("value", meta.get("key"));
      assertNull(meta.get("key2"));
      assertTrue(meta.containsKey("key"));
      assertFalse(meta.containsKey("keys"));

    }

    @Test
    void testWithMethods() {
      var base = TransactionMetadata.manual(AssetType.CRYPTO);

      // 'with' should return a NEW instance, leaving the old one untouched
      var updated = base.with("broker_id", "123");

      assertNotSame(base, updated);
      assertTrue(base.isEmpty());
      assertEquals("123", updated.get("broker_id"));
      assertEquals("MANUAL", updated.source());

      Map<String, String> additionalData = new HashMap<>();
      additionalData.put("key", "value");
      TransactionMetadata newData = base.withAll(additionalData);
      assertTrue(newData.containsKey("key"));
    }

    @Test
    void testAsFlatMap() {
      var meta = new TransactionMetadata(AssetType.STOCK, null, null, null);

      Map<String, String> map = meta.asFlatMap();
      assertEquals("STOCK", map.get("assetType"));
      assertEquals("UNKNOWN", map.get("source"));
      assertEquals("false", map.get("excluded"));
    }

    @Test
    void testAsFlatMapExclusionTrue() {
      Instant now = Instant.now();
      UserId userId = UserId.random();
      TransactionMetadata.ExclusionRecord exclusionRecord = new ExclusionRecord(now, userId, null);
      var meta = new TransactionMetadata(AssetType.STOCK, null, exclusionRecord, null);

      Map<String, String> map = meta.asFlatMap();
      assertEquals("STOCK", map.get("assetType"));
      assertEquals("UNKNOWN", map.get("source"));
      assertEquals("true", map.get("excluded"));
      assertEquals(now.toString(), map.get("excludedAt"));
      assertEquals(userId.id().toString(), map.get("excludedBy"));
    }

    @Test
    void testAsFlatMapExclusionTrue_WithComments() {
      Instant now = Instant.now();
      UserId userId = UserId.random();
      TransactionMetadata.ExclusionRecord record = new ExclusionRecord(now, userId, "for testing");
      var meta = new TransactionMetadata(AssetType.STOCK, null, record, null);

      Map<String, String> map = meta.asFlatMap();
      assertEquals("STOCK", map.get("assetType"));
      assertEquals("UNKNOWN", map.get("source"));
      assertEquals("true", map.get("excluded"));
      assertEquals(now.toString(), map.get("excludedAt"));
      assertEquals(userId.id().toString(), map.get("excludedBy"));
      assertEquals("for testing", map.get("excludedReason"));
    }

    @Test
    void testStaticFactories() {
      var csv = TransactionMetadata.csvImport(AssetType.STOCK, "trades.csv");

      assertEquals("CSV_IMPORT", csv.source());
      assertEquals("trades.csv", csv.get("filename"));
    }
  }
}
