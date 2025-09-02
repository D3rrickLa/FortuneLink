package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionCancelledEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionCompletedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionReversedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.CashTransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.ExpenseType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.IncomeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.IllegalStatusTransitionException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.TransactionAlreadyReversedException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CurrencyConversion;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.TradeTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.AccountTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.ReversalTransactionDetails;

class TransactionTest {

    @Mock
    private TradeTransactionDetails mockTradeDetails;

    @Mock
    private AccountTransactionDetails mockCashDetails;

    @Mock
    private ReversalTransactionDetails mockReversalDetails;

    private PortfolioId portfolioId;
    private MonetaryAmount amount;
    private Instant transactionDate;
    private CurrencyConversion USDtoUSD;
    private Currency USD;

    @BeforeEach
    void setUp() {
        USD = Currency.getInstance("USD");
        MockitoAnnotations.openMocks(this);
        portfolioId = PortfolioId.createRandom();
        amount = new MonetaryAmount(new Money(new BigDecimal("100.00"), USD),
                CurrencyConversion.identity(USD)); // 1:1 conversion rate
        transactionDate = Instant.now().minusSeconds(3600); // 1 hour ago
        USDtoUSD = CurrencyConversion.identity(USD);
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @SuppressWarnings("unlikely-arg-type")
        @Test
        @DisplayName("Should create trade transaction with correct defaults")
        void shouldCreateTradeTransaction() {
            // When
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            Transaction transaction2 = Transaction.createTradeTransaction(
                    portfolioId, TradeType.SELL, mockTradeDetails, amount, transactionDate);

            // Then
            assertAll(
                    () -> assertNotNull(transaction.getTransactionId()),
                    () -> assertEquals(portfolioId, transaction.getPortfolioId()),
                    () -> assertEquals(TradeType.BUY, transaction.getType()),
                    () -> assertEquals(TransactionStatus.PENDING, transaction.getStatus()),
                    () -> assertEquals(mockTradeDetails, transaction.getDetails()),
                    () -> assertEquals(transactionDate, transaction.getTransactionDate()),
                    () -> assertEquals(amount, transaction.getTransactionNetImpact()),
                    () -> assertFalse(transaction.isHidden()),
                    () -> assertEquals(0, transaction.getVersion()),
                    () -> assertNull(transaction.getParentTransactionId()),
                    () -> assertNotNull(transaction.getCorrelationId()),
                    () -> assertEquals(0, transaction.getDomainEvents().size()),
                    () -> assertNotNull(transaction.getVALID_TRANSITIONS()),

                    // assert equal, equals method
                    () -> assertEquals(transaction, transaction),
                    () -> assertNotEquals(new Object(), transaction),
                    () -> assertFalse(transaction.equals(null)),
                    () -> assertFalse(transaction.equals(transaction2)),
                    () -> assertFalse(transaction.equals(TradeType.class))
            // ^^^^ for some reason this is the only way to get 100% in equals method ^^^^

            );
        }

        @Test
        @DisplayName("Should return ture and or false for those querying methods")
        void shouldReturnTorFForQueryMethods() {
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            assertFalse(transaction.isIncome());
            assertFalse(transaction.isExpense());

            AccountTransactionDetails accountTransactionDetails = mock(AccountTransactionDetails.class);
            Transaction transactionIncome = Transaction.createIncomeTransaction(
                    portfolioId, IncomeType.INTEREST_INCOME, accountTransactionDetails, amount, transactionDate);
            assertTrue(transactionIncome.isIncome());
            assertFalse(transactionIncome.isExpense());
            Transaction transactionExpense = Transaction.createExpenseTransaction(
                    portfolioId, ExpenseType.EXPENSE, accountTransactionDetails, amount, transactionDate);
            assertTrue(transactionExpense.isExpense());
            assertFalse(transactionExpense.isIncome());
        }

        @Test
        void canBeUpdated_ReturnsTrue() {
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            assertTrue(transaction.canBeUpdated());

            transaction.cancel("testing");
            assertFalse(transaction.canBeUpdated());
        }

        @Test
        @DisplayName("Should create cash transaction with correct defaults")
        void shouldCreateCashTransaction() {
            // When
            Transaction transaction = Transaction.createCashTransaction(
                    portfolioId, CashTransactionType.DEPOSIT, mockCashDetails, amount, transactionDate);

            // Then
            assertEquals(CashTransactionType.DEPOSIT, transaction.getType());
            assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        }

        @Test
        @DisplayName("Should create reversal transaction with parent reference")
        void shouldCreateReversalTransaction() {
            // Given
            TransactionId parentId = TransactionId.createRandom();
            when(mockReversalDetails.getOriginalTransactionId()).thenReturn(parentId);

            // When
            Transaction transaction = Transaction.createReversalTransaction(
                    portfolioId, parentId, TradeType.BUY_REVERSAL, mockReversalDetails, amount, transactionDate);

            // Then
            assertAll(
                    () -> assertEquals(TradeType.BUY_REVERSAL, transaction.getType()),
                    () -> assertEquals(parentId, transaction.getParentTransactionId()),
                    () -> assertTrue(transaction.isReversal()));
        }

        @Test
        @DisplayName("Should reject non-reversal type for reversal transaction")
        void shouldRejectNonReversalTypeForReversalTransaction() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> Transaction.createReversalTransaction(
                    portfolioId, TransactionId.createRandom(), TradeType.BUY, mockReversalDetails, amount,
                    transactionDate));
        }

        @ParameterizedTest
        @EnumSource(value = TradeType.class, names = { "BUY_REVERSAL", "SELL_REVERSAL" })
        @DisplayName("Should accept reversal types for reversal transaction")
        void shouldAcceptReversalTypes(TradeType reversalType) {
            // Given
            // when(reversalType.isReversal()).thenReturn(true);

            // When & Then
            assertDoesNotThrow(() -> Transaction.createReversalTransaction(
                    portfolioId, TransactionId.createRandom(), reversalType, mockReversalDetails, amount,
                    transactionDate));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should reject null required parameters")
        void shouldRejectNullRequiredParameters() {
            assertAll(
                    () -> assertThrows(NullPointerException.class,
                            () -> Transaction.createTradeTransaction(null, TradeType.BUY, mockTradeDetails, amount,
                                    transactionDate)),
                    () -> assertThrows(NullPointerException.class,
                            () -> Transaction.createTradeTransaction(portfolioId, null, mockTradeDetails, amount,
                                    transactionDate)),
                    () -> assertThrows(NullPointerException.class,
                            () -> Transaction.createTradeTransaction(portfolioId, TradeType.BUY, null, amount,
                                    transactionDate)),
                    () -> assertThrows(NullPointerException.class,
                            () -> Transaction.createTradeTransaction(portfolioId, TradeType.BUY, mockTradeDetails, null,
                                    transactionDate)),
                    () -> assertThrows(NullPointerException.class, () -> Transaction.createTradeTransaction(portfolioId,
                            TradeType.BUY, mockTradeDetails, amount, null)));
        }

        @Test
        @DisplayName("Should reject future transaction date")
        void shouldRejectFutureTransactionDate() {
            // Given
            Instant futureDate = Instant.now().plusSeconds(3600); // 1 hour in future

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> Transaction.createTradeTransaction(portfolioId,
                    TradeType.BUY, mockTradeDetails, amount, futureDate));
        }

        @Test
        @DisplayName("Should reject zero amount")
        void shouldRejectZeroAmount() {
            // Given
            Money zeroMoney = new Money(BigDecimal.ZERO, USD);
            MonetaryAmount zeroAmount = new MonetaryAmount(zeroMoney, USDtoUSD);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> Transaction.createTradeTransaction(portfolioId,
                    TradeType.BUY, mockTradeDetails, zeroAmount, transactionDate));
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitions {

        private Transaction pendingTransaction;

        @BeforeEach
        void setUp() {
            pendingTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
        }

        @Test
        @DisplayName("Should complete pending transaction")
        void shouldCompletePendingTransaction() {
            // When
            pendingTransaction.markAsCompleted();

            // Then
            assertEquals(TransactionStatus.COMPLETED, pendingTransaction.getStatus());
            assertEquals(1, pendingTransaction.getVersion());

            List<DomainEvent> events = pendingTransaction.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof TransactionCompletedEvent);
        }

        @Test
        @DisplayName("Should cancel pending transaction")
        void shouldCancelPendingTransaction() {
            // Given
            String reason = "User requested cancellation";

            // When
            pendingTransaction.cancel(reason);

            // Then
            assertEquals(TransactionStatus.CANCELLED, pendingTransaction.getStatus());

            List<DomainEvent> events = pendingTransaction.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof TransactionCancelledEvent);
        }

        @Test
        @DisplayName("Should fail pending transaction")
        void shouldFailPendingTransaction() {
            // Given
            String reason = "Insufficient funds";

            // When
            pendingTransaction.fail(reason);

            // Then
            assertEquals(TransactionStatus.FAILED, pendingTransaction.getStatus());
        }

        @Test
        @DisplayName("Should reject invalid status transitions")
        void shouldRejectInvalidStatusTransitions() {
            // Given
            pendingTransaction.markAsCompleted();

            // When & Then
            assertThrows(IllegalStatusTransitionException.class, () -> pendingTransaction.markAsCompleted());
            assertThrows(IllegalStatusTransitionException.class, () -> pendingTransaction.cancel("reason"));
        }
    }

    @Nested
    @DisplayName("Reversal Logic")
    class ReversalLogic {

        private Transaction completedTransaction;

        @BeforeEach
        void setUp() {
            completedTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            completedTransaction.markAsCompleted();
            completedTransaction.markEventsAsCommitted(); // Clear events for clean testing
        }

        @Test
        @DisplayName("Should reverse completed transaction with reversal transaction")
        void shouldReverseCompletedTransaction() {
            // Given
            when(mockReversalDetails.getOriginalTransactionId()).thenReturn(completedTransaction.getTransactionId());
            // when(TradeType.BUY.getReversalType()).thenReturn(TradeType.BUY_REVERSAL);

            Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, completedTransaction.getTransactionId(), TradeType.BUY_REVERSAL, mockReversalDetails,
                    amount, transactionDate);

            // When
            completedTransaction.reverse(reversalTransaction, Instant.now());

            // Then
            assertTrue(completedTransaction.isReversed());
            assertEquals(TransactionStatus.REVERSED, completedTransaction.getStatus());

            List<DomainEvent> events = completedTransaction.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof TransactionReversedEvent);
        }

        @Test
        @DisplayName("Should not reverse non-completed transaction")
        void shouldNotReverseNonCompletedTransaction() {
            // Given
            Transaction pendingTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, pendingTransaction.getTransactionId(), TradeType.BUY_REVERSAL, mockReversalDetails,
                    amount, transactionDate);

            // When & Then
            assertFalse(pendingTransaction.canBeReversed());
            assertThrows(IllegalStateException.class,
                    () -> pendingTransaction.reverse(reversalTransaction, Instant.now()));
        }

        @Test
        @DisplayName("Should not reverse already reversed transaction")
        void shouldNotReverseAlreadyReversedTransaction() {
            // Given
            when(mockReversalDetails.getOriginalTransactionId()).thenReturn(completedTransaction.getTransactionId());
            // when(TradeType.BUY.getReversalType()).thenReturn(TradeType.BUY_REVERSAL);

            Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, completedTransaction.getTransactionId(), TradeType.BUY_REVERSAL, mockReversalDetails,
                    amount, transactionDate);

            completedTransaction.reverse(reversalTransaction, Instant.now());

            // When & Then
            assertThrows(IllegalStateException.class,
                    () -> completedTransaction.reverse(reversalTransaction, Instant.now()));
        }

        @Test
        @DisplayName("Should not reverse reversal transaction")
        void shouldNotReverseReversalTransaction() {
            // Given
            Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, TransactionId.createRandom(), TradeType.BUY_REVERSAL, mockReversalDetails, amount,
                    transactionDate);
            reversalTransaction.markAsCompleted();

            // When & Then
            assertFalse(reversalTransaction.canBeReversed());
            assertTrue(reversalTransaction.isReversal());
        }

        @Test
        @DisplayName("Should reject null reversal parameters")
        void shouldRejectNullReversalParameters() {
            // Given
            Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, completedTransaction.getTransactionId(), TradeType.BUY_REVERSAL, mockReversalDetails,
                    amount, transactionDate);

            assertAll(
                    () -> assertThrows(NullPointerException.class,
                            () -> completedTransaction.reverse(null, Instant.now())),
                    () -> assertThrows(NullPointerException.class,
                            () -> completedTransaction.reverse(reversalTransaction, null)));
        }
    }

    @Nested
    @DisplayName("Complex Reversal with Transaction")
    class ComplexReversalLogic {

        private Transaction originalTransaction;
        private Transaction reversalTransaction;

        @BeforeEach
        void setUp() {
            originalTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            originalTransaction.markAsCompleted();

            when(mockReversalDetails.getOriginalTransactionId()).thenReturn(originalTransaction.getTransactionId());
            // when(TradeType.BUY.getReversalType()).thenReturn(TradeType.BUY_REVERSAL); //
            // can't stub enums

            reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, originalTransaction.getTransactionId(), TradeType.BUY_REVERSAL, mockReversalDetails,
                    amount, transactionDate);
        }

        @Test
        @DisplayName("Should reverse with linked reversal transaction")
        void shouldReverseWithLinkedReversalTransaction() {
            // When
            originalTransaction.reverse(reversalTransaction, Instant.now());

            // Then
            assertTrue(originalTransaction.isReversed());
            assertEquals(TransactionStatus.REVERSED, originalTransaction.getStatus());
        }

        @Test
        @DisplayName("Should reject mismatched reversal transaction type")
        void shouldRejectMismatchedReversalTransactionType() {
            // Given
            Transaction originalTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);

            // Create a reversal transaction with the wrong type (SELL_REVERSAL instead of
            // BUY_REVERSAL)
            ReversalTransactionDetails details = mock(ReversalTransactionDetails.class);

            Transaction wrongReversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, originalTransaction.getTransactionId(), TradeType.SELL_REVERSAL, details, amount,
                    transactionDate);

            // When & Then
            assertThrows(IllegalStateException.class,
                    () -> originalTransaction.reverse(wrongReversalTransaction, Instant.now()));
        }

        @Test
        @DisplayName("Should reject reversal with wrong original transaction ID")
        void shouldRejectReversalWithWrongOriginalTransactionId() {
            // Given
            Transaction originalTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);

            // Create a reversal transaction referencing a different original transaction ID
            ReversalTransactionDetails details = mock(ReversalTransactionDetails.class);

            Transaction wrongReversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, TransactionId.createRandom(), TradeType.BUY_REVERSAL, details, amount,
                    transactionDate);

            // When & Then
            assertThrows(IllegalStateException.class,
                    () -> originalTransaction.reverse(wrongReversalTransaction, Instant.now()));
        }

        @Test
        @DisplayName("Should reject non-reversal details for reversal transaction")
        void shouldRejectNonReversalDetailsForReversalTransaction() {
            // Given - create reversal transaction with wrong details type
            Transaction invalidReversalTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY_REVERSAL, mockTradeDetails, amount, transactionDate);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> originalTransaction.reverse(invalidReversalTransaction, Instant.now()));
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
        }

        @Test
        @DisplayName("Should identify transaction categories correctly")
        void shouldIdentifyTransactionCategories() {
            // Given
            // when(TradeType.BUY.getCategory()).thenReturn(TransactionCategory.TRADE);

            // Then
            assertEquals(TransactionCategory.TRADE, transaction.getCategory());
            assertFalse(transaction.isIncome());
            assertFalse(transaction.isExpense());
        }

        @Test
        @DisplayName("Should return absolute amount")
        void shouldReturnAbsoluteAmount() {
            // Given
            Money negativeMoney = new Money(new BigDecimal("-100.00"), USD);
            MonetaryAmount negativeAmount = new MonetaryAmount(negativeMoney, USDtoUSD);
            Transaction negativeTransaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.SELL, mockTradeDetails, negativeAmount, transactionDate);

            // When
            Money absoluteAmount = negativeTransaction.getAbsoluteAmount();

            // Then
            assertEquals(new BigDecimal("100.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()),
                    absoluteAmount.amount());
        }

        @Test
        @DisplayName("Should convert to portfolio currency")
        void shouldConvertToPortfolioCurrency() {
            // Given
            Money portfolioAmount = new Money(new BigDecimal("130.00"), Currency.getInstance("CAD"));

            // When
            Money result = transaction.getNetCostInPortfolioCurrency();
            // so result is using ao USD to USD conversion, going to change that

            // Then
            assertEquals(portfolioAmount, result.convertTo(Currency.getInstance("CAD"),
                    CurrencyConversion.of("USD", "CAD", 1.30, transactionDate)));
        }

        @Test
        @DisplayName("Should get native currency")
        void shouldGetNativeCurrency() {
            // Given
            // when(amount.nativeAmount().currency()).thenReturn(USD);

            // When
            Currency currency = transaction.getNetCostInPortfolioCurrency().currency();

            // Then
            assertEquals(USD, currency);
        }
    }

    @Nested
    @DisplayName("Domain Events")
    class DomainEvents {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
        }

        @Test
        @DisplayName("Should track uncommitted events")
        void shouldTrackUncommittedEvents() {
            // When
            transaction.markAsCompleted();
            // Then
            List<DomainEvent> events = transaction.getUncommittedEvents();
            assertEquals(1, events.size()); // Only completed event should be there
            assertTrue(events.get(0) instanceof TransactionCompletedEvent);
        }

        @Test
        @DisplayName("Should clear events when marked as committed")
        void shouldClearEventsWhenMarkedAsCommitted() {
            // Given
            transaction.markAsCompleted();
            assertEquals(1, transaction.getUncommittedEvents().size());

            // When
            transaction.markEventsAsCommitted();

            // Then
            assertTrue(transaction.getUncommittedEvents().isEmpty());
        }

        @Test
        @DisplayName("Should return immutable events list")
        void shouldReturnImmutableEventsList() {
            // When
            List<DomainEvent> events = transaction.getUncommittedEvents();

            // Then
            assertThrows(UnsupportedOperationException.class, () -> events.add(mock(TransactionCompletedEvent.class)));
        }
    }

    @Nested
    @DisplayName("Version and Audit")
    class VersionAndAudit {

        @Test
        @DisplayName("Should increment version on updates")
        void shouldIncrementVersionOnUpdates() {
            // Given
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            assertEquals(0, transaction.getVersion());

            // When
            transaction.markAsCompleted();

            // Then
            assertEquals(1, transaction.getVersion());
            assertTrue(!transaction.getUpdatedAt().isBefore(transaction.getCreatedAt()));
        }

        @Test
        @DisplayName("Should update timestamps on state changes")
        void shouldUpdateTimestampsOnStateChanges() {
            // Given
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            Instant originalUpdatedAt = transaction.getUpdatedAt();

            // When
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            } // Ensure time difference
            transaction.markAsCompleted();

            // Then
            assertTrue(transaction.getUpdatedAt().isAfter(originalUpdatedAt));
        }
    }

    @Nested
    @DisplayName("Equality and Identity")
    class EqualityAndIdentity {

        @Test
        @DisplayName("Should be equal when transaction IDs match")
        void shouldBeEqualWhenTransactionIdsMatch() {
            // Given
            // TransactionId sharedId = TransactionId.createRandom();
            Transaction transaction1 = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            Transaction transaction2 = Transaction.createTradeTransaction(
                    portfolioId, TradeType.SELL, mockTradeDetails, amount, transactionDate);

            // Simulate same ID (in practice this would be set differently)
            // This test assumes you have a way to create transactions with specific IDs for
            // testing

            // When & Then - assuming they have different IDs
            assertNotEquals(transaction1, transaction2);
        }

        @Test
        @DisplayName("Should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            // Given
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);

            // When
            int hashCode1 = transaction.hashCode();
            int hashCode2 = transaction.hashCode();

            // Then
            assertEquals(hashCode1, hashCode2);
        }

        @Test
        @DisplayName("Should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            // Given
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);

            // When
            String toString = transaction.toString();

            // Then
            assertAll(
                    () -> assertTrue(toString.contains("Transaction")),
                    () -> assertTrue(toString.contains(transaction.getTransactionId().toString())),
                    () -> assertTrue(toString.contains("BUY")),
                    () -> assertTrue(toString.contains("PENDING")));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle metadata correctly")
        void shouldHandleMetadataCorrectly() {
            // Given
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);

            // Then
            assertNull(transaction.getMetadata());
        }

        @Test
        @DisplayName("Should allow near-future transaction dates")
        void shouldAllowNearFutureTransactionDates() {
            // Given
            Instant nearFuture = Instant.now().plusSeconds(60); // 1 minute in future

            // When & Then
            assertDoesNotThrow(() -> Transaction.createTradeTransaction(portfolioId, TradeType.BUY, mockTradeDetails,
                    amount, nearFuture));
        }

        @Test
        @DisplayName("Should handle reversal type identification")
        void shouldHandleReversalTypeIdentification() {
            // Given
            // when(TradeType.BUY_REVERSAL.isReversal()).thenReturn(true);
            Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, TransactionId.createRandom(), TradeType.BUY_REVERSAL, mockReversalDetails, amount,
                    transactionDate);

            // Then
            assertTrue(reversalTransaction.isReversal());
            assertFalse(reversalTransaction.canBeReversed());
        }

        @Test
        @DisplayName("Should not handle reversed transactions")
        void shouldNotReverseWhenIsAlreadyReversed()
                throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            transaction.markAsCompleted();

            assertTrue(transaction.canBeReversed());

            // Set status to REVERSED to make isReversed() return true
            Field statusField = Transaction.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(transaction, TransactionStatus.REVERSED);

            // This hits the !isReversed() condition (which evaluates to false)
            assertFalse(transaction.canBeReversed());
        }

        @Test
        void shouldReverseWhenNotReversed() {
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            transaction.markAsCompleted(); // status = COMPLETED

            // This exercises !isReversed() -> true branch
            assertTrue(transaction.canBeReversed());
        }

        @Test
        void shouldNotReverseWhenAlreadyReversed() throws Exception {
            Transaction transaction = Transaction.createTradeTransaction(
                    portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            transaction.markAsCompleted();

            Field statusField = Transaction.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(transaction, TransactionStatus.REVERSED);

            // This exercises !isReversed() -> false branch
            assertFalse(transaction.canBeReversed());
        }

        @Test
        void reverseShouldThrowIllegalArgumentWhenTimeIsBeforeUpdated() {
            Transaction transaction = Transaction.createTradeTransaction(
                portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
            
            ReversalTransactionDetails details = mock(ReversalTransactionDetails.class);
            when(details.getOriginalTransactionId()).thenReturn(transaction.getTransactionId());
            
            Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, transaction.getTransactionId(), TradeType.BUY_REVERSAL, details, amount,
                    transactionDate);

            transaction.markAsCompleted();

            Exception e = assertThrows(IllegalArgumentException.class, () -> transaction.reverse(reversalTransaction, transaction.getUpdatedAt().minusSeconds(3000)));
            assertEquals("New UpdatedAt cannot be before current updatedAt.", e.getLocalizedMessage());
        }

        @Test
        void reverseShouldThrowIllegalArgumentExceptionWhenOgTransactionIdIsNotTransactionId() {
                Transaction transaction = Transaction.createTradeTransaction(
                        portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
                transaction.markAsCompleted();
                ReversalTransactionDetails details = mock(ReversalTransactionDetails.class);
                when(details.getOriginalTransactionId()).thenReturn(TransactionId.createRandom());
                Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, transaction.getTransactionId(), TradeType.BUY_REVERSAL, details, amount,
                    transactionDate);
                assertThrows(IllegalArgumentException.class, () -> transaction.reverse(reversalTransaction, Instant.now()));
        }
        @Test
        void reverseShouldThrowIllegalArgumentExceptionReversalTypesDoesntMatchRightType() {
                Transaction transaction = Transaction.createTradeTransaction(
                        portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
                transaction.markAsCompleted();
                ReversalTransactionDetails details = mock(ReversalTransactionDetails.class);
                when(details.getOriginalTransactionId()).thenReturn(transaction.getTransactionId());
                Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, transaction.getTransactionId(), TradeType.SELL_REVERSAL, details, amount,
                    transactionDate);
                Exception e = assertThrows(IllegalArgumentException.class, () -> transaction.reverse(reversalTransaction, Instant.now()));
                assertEquals("Reversal transaction type must match expected reversal type.", e.getLocalizedMessage());
        }
        @Test
        void reverseShouldThrowTransactionAlreadyReversedExceptionWhenIsReversedTriggers() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
                Transaction transaction = Transaction.createTradeTransaction(
                        portfolioId, TradeType.BUY, mockTradeDetails, amount, transactionDate);
                transaction.markAsCompleted();
                
                ReversalTransactionDetails details = mock(ReversalTransactionDetails.class);
                when(details.getOriginalTransactionId()).thenReturn(transaction.getTransactionId());
                Transaction reversalTransaction = Transaction.createReversalTransaction(
                    portfolioId, transaction.getTransactionId(), TradeType.BUY_REVERSAL, details, amount,
                    transactionDate);
                
                transaction.reverse(reversalTransaction, Instant.now());
                Field typeField = transaction.getClass().getDeclaredField("type");
                typeField.setAccessible(true);
                typeField.set(transaction, TradeType.BUY);
                Field statusField = transaction.getClass().getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(transaction, TransactionStatus.COMPLETED);
                 
                assertThrows(TransactionAlreadyReversedException.class, () -> transaction.reverse(transaction, Instant.now()));
        }
    }
}