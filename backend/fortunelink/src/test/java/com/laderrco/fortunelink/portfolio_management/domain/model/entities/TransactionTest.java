package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction.TransactionMetadata;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee.FeeMetadata;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.TransactionDate;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;

public class TransactionTest {

    @Nested
    @DisplayName("Constructor tests")
    public class ConstructorTests {

        @ParameterizedTest
        @EnumSource(value = TransactionType.class, names = { "BUY", "SELL" })
        void testConstructor_Success(TransactionType type) {
            TransactionId transactionId = TransactionId.newId();
            AccountId accountId = AccountId.newId();
            TransactionType transactionType = type;
            TradeExecution execution = new TradeExecution(
                    new AssetSymbol("AAPL"),
                    new Quantity(BigDecimal.TEN),
                    Money.of(135, "USD"));
            Money cashDelta = Money.of(-1350, "USD");
            List<Fee> fees = List.of();
            String notes = "Some notes";
            TransactionDate occurredAt = TransactionDate.now();
            TransactionId relatedTransactionId = null;
            TransactionMetadata metadata = null;

            Transaction transaction = new Transaction(transactionId, accountId, transactionType, execution, cashDelta,
                    fees,
                    notes, occurredAt, relatedTransactionId, metadata);
            assertAll(
                    () -> assertEquals(transactionId, transaction.transactionId()));
        }

        @Test
        void testConstructor_SuccessNotExecution() {
            TransactionId transactionId = TransactionId.newId();
            AccountId accountId = AccountId.newId();
            TransactionType transactionType = TransactionType.INTEREST;
            TradeExecution execution = null;
            Money cashDelta = Money.of(1350, "USD");
            List<Fee> fees = List.of();
            String notes = "Some notes";
            TransactionDate occurredAt = TransactionDate.now();
            TransactionId relatedTransactionId = null;
            TransactionMetadata metadata = null;

            Transaction transaction = new Transaction(transactionId, accountId, transactionType, execution, cashDelta,
                    fees,
                    notes, occurredAt, relatedTransactionId, metadata);
            assertAll(
                    () -> assertEquals(transactionId, transaction.transactionId()));
        }

        @Test
        void testConstructor_Failure_WhenExecutionIsNullAndRequireExecutionIsTrue() {
            TransactionId transactionId = TransactionId.newId();
            AccountId accountId = AccountId.newId();
            TransactionType transactionType = TransactionType.BUY;
            TradeExecution execution = null;
            Money cashDelta = Money.of(1350, "USD");
            List<Fee> fees = List.of();
            String notes = "Some notes";
            TransactionDate occurredAt = TransactionDate.now();
            TransactionId relatedTransactionId = null;
            TransactionMetadata metadata = null;

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Transaction(transactionId, accountId, transactionType, execution, cashDelta, fees,
                            notes, occurredAt, relatedTransactionId, metadata));

            assertTrue(ex.getMessage().contains("requires execution details"));
        }

        @Test
        void testConstructor_Failure_WhenExecutionIsNonNullAndTypeIsNotValid() {
            TransactionId transactionId = TransactionId.newId();
            AccountId accountId = AccountId.newId();
            TransactionType transactionType = TransactionType.DEPOSIT;
            TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"), new Quantity(BigDecimal.TEN),
                    Money.of(135, "USD"));
            Money cashDelta = Money.of(1350, "USD");
            List<Fee> fees = List.of();
            String notes = "Some notes";
            TransactionDate occurredAt = TransactionDate.now();
            TransactionId relatedTransactionId = null;
            TransactionMetadata metadata = null;

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Transaction(transactionId, accountId, transactionType, execution, cashDelta, fees,
                            notes, occurredAt, relatedTransactionId, metadata));

            assertTrue(ex.getMessage().contains("cannot have execution details"));
        }

        @Nested
        @DisplayName("validateTradeConsistencyTests")
        public class ValidateTradeConsistencyTests {
            @Test
            void testValidateTradeConsistency_Failure_WhenCashOutNotEqualToIsBuy() {
                TransactionId transactionId = TransactionId.newId();
                AccountId accountId = AccountId.newId();
                TransactionType transactionType = TransactionType.BUY;
                TradeExecution execution = new TradeExecution(new AssetSymbol("AAPL"), new Quantity(BigDecimal.TEN),
                        Money.of(135, "USD"));
                Money cashDelta = Money.of(1350, "USD");
                List<Fee> fees = List.of();
                String notes = "Some notes";
                TransactionDate occurredAt = TransactionDate.now();
                TransactionId relatedTransactionId = null;
                TransactionMetadata metadata = null;

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> new Transaction(transactionId, accountId, transactionType, execution, cashDelta, fees,
                                notes, occurredAt, relatedTransactionId, metadata));

                assertTrue(ex.getMessage().contains("Cash delta sign does not match trade direction"));
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
            TradeExecution execution = new TradeExecution(
                    new AssetSymbol("AAPL"),
                    new Quantity(BigDecimal.TEN),
                    Money.of(135, "USD"));
            Money cashDelta = Money.of(-1350, "USD");
            List<Fee> fees = List.of(
                    new Fee(FeeType.BROKERAGE, Money.of(5, "USD"), Money.of(5, "USD"),
                            ExchangeRate.identity(Currency.USD, Instant.now()), Instant.now(),
                            new FeeMetadata(Map.of())),
                    new Fee(FeeType.BROKERAGE, Money.of(5, "CAD"), Money.of(3.25, "USD"),
                            new ExchangeRate(Currency.CAD, Currency.USD, BigDecimal.valueOf(1.35), Instant.now()),
                            Instant.now(), new FeeMetadata(Map.of()))

            );
            String notes = "Some notes";
            TransactionDate occurredAt = TransactionDate.now();
            TransactionId relatedTransactionId = null;
            TransactionMetadata metadata = null;

            Transaction transaction = new Transaction(transactionId, accountId, transactionType, execution, cashDelta,
                    fees,
                    notes, occurredAt, relatedTransactionId, metadata);
            assertAll(
                    () -> assertEquals(Money.of(8.25, "USD"), transaction.totalFeesInAccountCurrency()));
        }

    }

}
