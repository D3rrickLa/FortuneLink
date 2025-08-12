package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TransactionDetails;

public class TransactionTest {
    private TransactionId transactionId;
    private CorrelationId correlationId;
    private TransactionId parentTransactionId;
    private PortfolioId portfolioId;
    private TransactionDetails transactionDetails;
    private Money transactionNetImpact;
    private Instant transactionDate;
    private Instant createdAt;

    private Currency USD;

    @BeforeEach
    void setUp() {
        USD = Currency.getInstance("USD");
        transactionId = new TransactionId(UUID.randomUUID());
        correlationId = new CorrelationId(UUID.randomUUID());
        parentTransactionId = new TransactionId(UUID.randomUUID());
        portfolioId = new PortfolioId(UUID.randomUUID());
        transactionDetails = new CashflowTransactionDetails(new Money(2000, "USD"), CashflowType.DEPOSIT, TransactionSource.SYSTEM, "Test transaction", Collections.emptyList());
        transactionNetImpact = new Money(BigDecimal.valueOf(100.00), USD);
        transactionDate = Instant.now();
        createdAt = Instant.now();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create transaction with all valid parameters")
        void shouldCreateTransactionWithValidParameters() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            assertAll(
                () -> assertEquals(transactionId, transaction.getTransactionId()),
                () -> assertEquals(correlationId, transaction.getCorrelationId()),
                () -> assertEquals(parentTransactionId, transaction.getParentTransactionId()),
                () -> assertEquals(portfolioId, transaction.getPortfolioId()),
                () -> assertEquals(TransactionType.DEPOSIT, transaction.getType()),
                () -> assertEquals(TransactionStatus.PENDING, transaction.getStatus()),
                () -> assertEquals(transactionDetails, transaction.getTransactionDetails()),
                () -> assertEquals(transactionNetImpact, transaction.getTransactionNetImpact()),
                () -> assertEquals(transactionDate, transaction.getTransactionDate()),
                () -> assertEquals(createdAt, transaction.getCreatedAt()),
                () -> assertEquals(createdAt, transaction.getUpdatedAt()),
                () -> assertFalse(transaction.isHidden()),
                () -> assertEquals(1, transaction.getVersion())
            );
        }

        @Test
        @DisplayName("Should create transaction with null parent transaction ID")
        void shouldCreateTransactionWithNullParentId() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, null, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            assertNull(transaction.getParentTransactionId());
        }

        @Test
        @DisplayName("Should throw exception when transaction ID is null")
        void shouldThrowExceptionWhenTransactionIdIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    null, correlationId, parentTransactionId, portfolioId,
                    TransactionType.DEPOSIT, TransactionStatus.PENDING,
                    transactionDetails, transactionNetImpact, transactionDate, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when correlation ID is null")
        void shouldThrowExceptionWhenCorrelationIdIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, null, parentTransactionId, portfolioId,
                    TransactionType.DEPOSIT, TransactionStatus.PENDING,
                    transactionDetails, transactionNetImpact, transactionDate, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when portfolio ID is null")
        void shouldThrowExceptionWhenPortfolioIdIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, correlationId, parentTransactionId, null,
                    TransactionType.DEPOSIT, TransactionStatus.PENDING,
                    transactionDetails, transactionNetImpact, transactionDate, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when transaction type is null")
        void shouldThrowExceptionWhenTransactionTypeIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, correlationId, parentTransactionId, portfolioId,
                    null, TransactionStatus.PENDING,
                    transactionDetails, transactionNetImpact, transactionDate, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when transaction status is null")
        void shouldThrowExceptionWhenTransactionStatusIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, correlationId, parentTransactionId, portfolioId,
                    TransactionType.DEPOSIT, null,
                    transactionDetails, transactionNetImpact, transactionDate, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when transaction details is null")
        void shouldThrowExceptionWhenTransactionDetailsIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, correlationId, parentTransactionId, portfolioId,
                    TransactionType.DEPOSIT, TransactionStatus.PENDING,
                    null, transactionNetImpact, transactionDate, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when transaction net impact is null")
        void shouldThrowExceptionWhenTransactionNetImpactIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, correlationId, parentTransactionId, portfolioId,
                    TransactionType.DEPOSIT, TransactionStatus.PENDING,
                    transactionDetails, null, transactionDate, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when transaction date is null")
        void shouldThrowExceptionWhenTransactionDateIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, correlationId, parentTransactionId, portfolioId,
                    TransactionType.DEPOSIT, TransactionStatus.PENDING,
                    transactionDetails, transactionNetImpact, null, createdAt
                );
            });
        }

        @Test
        @DisplayName("Should throw exception when created at is null")
        void shouldThrowExceptionWhenCreatedAtIsNull() {
            assertThrows(NullPointerException.class, () -> {
                new Transaction(
                    transactionId, correlationId, parentTransactionId, portfolioId,
                    TransactionType.DEPOSIT, TransactionStatus.PENDING,
                    transactionDetails, transactionNetImpact, transactionDate, null
                );
            });
        }
    }

    @Nested
    @DisplayName("Update Status Tests")
    class UpdateStatusTests {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
        }

        @Test
        @DisplayName("Should update status successfully")
        void shouldUpdateStatusSuccessfully() {
            Instant newUpdatedAt = createdAt.plusSeconds(60);
            transaction.updateStatus(TransactionStatus.COMPLETED, newUpdatedAt);

            assertAll(
                () -> assertEquals(TransactionStatus.COMPLETED, transaction.getStatus()),
                () -> assertEquals(newUpdatedAt, transaction.getUpdatedAt()),
                () -> assertEquals(2, transaction.getVersion())
            );
        }

        @Test
        @DisplayName("Should throw exception when new status is null")
        void shouldThrowExceptionWhenNewStatusIsNull() {
            assertThrows(NullPointerException.class, () -> {
                transaction.updateStatus(null, createdAt.plusSeconds(60));
            });
        }

        @Test
        @DisplayName("Should throw exception when updated at is null")
        void shouldThrowExceptionWhenUpdatedAtIsNull() {
            assertThrows(NullPointerException.class, () -> {
                transaction.updateStatus(TransactionStatus.COMPLETED, null);
            });
        }

        @Test
        @DisplayName("Should throw exception when updated at is before current updated at")
        void shouldThrowExceptionWhenUpdatedAtIsBeforeCurrent() {
            assertThrows(IllegalArgumentException.class, () -> {
                transaction.updateStatus(TransactionStatus.COMPLETED, createdAt.minusSeconds(60));
            });
        }
    }

    @Nested
    @DisplayName("Hide/Unhide Tests")
    class HideUnhideTests {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
        }

        @Test
        @DisplayName("Should hide transaction successfully")
        void shouldHideTransactionSuccessfully() {
            Instant newUpdatedAt = createdAt.plusSeconds(60);
            transaction.hide(newUpdatedAt);

            assertAll(
                () -> assertTrue(transaction.isHidden()),
                () -> assertEquals(newUpdatedAt, transaction.getUpdatedAt()),
                () -> assertEquals(2, transaction.getVersion())
            );
        }

        @Test
        @DisplayName("Should unhide transaction successfully")
        void shouldUnhideTransactionSuccessfully() {
            Instant hideTime = createdAt.plusSeconds(60);
            Instant unhideTime = hideTime.plusSeconds(60);
            
            transaction.hide(hideTime);
            transaction.unhide(unhideTime);

            assertAll(
                () -> assertFalse(transaction.isHidden()),
                () -> assertEquals(unhideTime, transaction.getUpdatedAt()),
                () -> assertEquals(3, transaction.getVersion())
            );
        }

        @Test
        @DisplayName("Should throw exception when hide updated at is null")
        void shouldThrowExceptionWhenHideUpdatedAtIsNull() {
            assertThrows(NullPointerException.class, () -> {
                transaction.hide(null);
            });
        }

        @Test
        @DisplayName("Should throw exception when unhide updated at is null")
        void shouldThrowExceptionWhenUnhideUpdatedAtIsNull() {
            assertThrows(NullPointerException.class, () -> {
                transaction.unhide(null);
            });
        }

        @Test
        @DisplayName("Should throw exception when hide updated at is before current")
        void shouldThrowExceptionWhenHideUpdatedAtIsBeforeCurrent() {
            assertThrows(IllegalArgumentException.class, () -> {
                transaction.hide(createdAt.minusSeconds(60));
            });
        }

        @Test
        @DisplayName("Should throw exception when unhide updated at is before current")
        void shouldThrowExceptionWhenUnhideUpdatedAtIsBeforeCurrent() {
            assertThrows(IllegalArgumentException.class, () -> {
                transaction.unhide(createdAt.minusSeconds(60));
            });
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should identify reversal transaction types correctly")
        void shouldIdentifyReversalTransactionTypesCorrectly() {
            Transaction reversalTxn = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.REVERSAL, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
            assertTrue(reversalTxn.isReversal());

            Transaction reversalBuyTxn = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.REVERSAL_BUY, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
            assertTrue(reversalBuyTxn.isReversal());
        }

        @Test
        @DisplayName("Should identify non-reversal transaction types correctly")
        void shouldIdentifyNonReversalTransactionTypesCorrectly() {
            Transaction depositTxn = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
            assertFalse(depositTxn.isReversal());

            Transaction withdrawalTxn = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.WITHDRAWAL, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
            assertFalse(withdrawalTxn.isReversal());
        }

        @Test
        @DisplayName("Should allow updates when transaction is pending")
        void shouldAllowUpdatesWhenTransactionIsPending() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
            assertTrue(transaction.canBeUpdated());
        }

        @Test
        @DisplayName("Should not allow updates when transaction is finalized")
        void shouldNotAllowUpdatesWhenTransactionIsFinalized() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.FINALIZED,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
            assertFalse(transaction.canBeUpdated());
        }

        @Test
        @DisplayName("Should not allow updates when transaction is cancelled")
        void shouldNotAllowUpdatesWhenTransactionIsCancelled() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.CANCELLED,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );
            assertFalse(transaction.canBeUpdated());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Should be equal when transaction IDs are same")
        void shouldBeEqualWhenTransactionIdsAreSame() {
            Transaction txn1 = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            Transaction txn2 = new Transaction(
                transactionId, new CorrelationId(UUID.randomUUID()), parentTransactionId, portfolioId,
                TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            assertEquals(txn1, txn2);
            assertEquals(txn1.hashCode(), txn2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when transaction IDs are different")
        void shouldNotBeEqualWhenTransactionIdsAreDifferent() {
            Transaction txn1 = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            Transaction txn2 = new Transaction(
                new TransactionId(UUID.randomUUID()), correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            assertNotEquals(txn1, txn2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            assertNotEquals(transaction, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            assertNotEquals(transaction, "not a transaction");
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            Transaction transaction = new Transaction(
                transactionId, correlationId, parentTransactionId, portfolioId,
                TransactionType.DEPOSIT, TransactionStatus.PENDING,
                transactionDetails, transactionNetImpact, transactionDate, createdAt
            );

            assertEquals(transaction, transaction);
        }
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        Transaction transaction = new Transaction(
            transactionId, correlationId, parentTransactionId, portfolioId,
            TransactionType.DEPOSIT, TransactionStatus.PENDING,
            transactionDetails, transactionNetImpact, transactionDate, createdAt
        );

        String toString = transaction.toString();
        
        assertAll(
            () -> assertTrue(toString.contains("Transaction{")),
            () -> assertTrue(toString.contains("transactionId=" + transactionId)),
            () -> assertTrue(toString.contains("type=" + TransactionType.DEPOSIT)),
            () -> assertTrue(toString.contains("status=" + TransactionStatus.PENDING)),
            () -> assertTrue(toString.contains("netImpact=" + transactionNetImpact)),
            () -> assertTrue(toString.contains("version=1"))
        );
    }
}
