package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PagedTransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.DateRangeView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionHistoryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

class TransactionDtoMapperTest {

    private TransactionDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionDtoMapper();
    }

    @Test
    @DisplayName("toResponse maps TransactionView to TransactionHttpResponse correctly")
    void toResponse_mapsCorrectly() {
        TransactionView transaction = new TransactionView(
                TransactionId.randomId(),
                TransactionType.BUY,
                "AAPL",
                BigDecimal.TEN,
                Money.of(BigDecimal.valueOf(100), "USD"),
                null, // no fees
                Money.of(BigDecimal.valueOf(1000), "USD"),
                Instant.now(),
                "Test notes");

        TransactionHttpResponse response = mapper.toResponse("account-123", transaction);

        assertEquals("account-123", response.accountId());
        assertEquals(transaction.transactionId().transactionId().toString(), response.id());
        assertEquals("BUY", response.transactionType());
        assertEquals("AAPL", response.symbol());
        assertEquals(BigDecimal.TEN, response.quantity());
        assertEquals(BigDecimal.valueOf(100).setScale(Precision.getMoneyPrecision()), response.price());
        assertEquals("USD", response.priceCurrency());
        assertEquals(BigDecimal.ZERO, response.fee()); // no fees
        assertEquals(transaction.totalCost().amount(), response.totalCost());
        assertEquals(transaction.notes(), response.notes());
    }

    @Test
    @DisplayName("toResponse correctly sums fees in base currency")
    void toResponse_sumsFeesCorrectly() {
        // Arrange
        TransactionId transactionId = TransactionId.randomId();
        Money price = Money.of(BigDecimal.valueOf(100), "USD");

        // Mock fees
        Fee fee1 = new Fee(FeeType.ACCOUNT_MAINTENANCE, Money.of(10, "USD"),
                ExchangeRate.createSingle(ValidatedCurrency.USD, "null"), null, Instant.now());
        Fee fee2 = new Fee(FeeType.ACCOUNT_MAINTENANCE, Money.of(5, "USD"),
                ExchangeRate.createSingle(ValidatedCurrency.USD, "null"), null, Instant.now());

        TransactionView transactionView = new TransactionView(
                transactionId,
                TransactionType.BUY,
                "AAPL",
                BigDecimal.TEN,
                price,
                List.of(fee1, fee2),
                Money.of(BigDecimal.valueOf(1000), "USD"), // totalCost
                Instant.now(),
                "Test note");

        // Act
        var response = mapper.toResponse("account-123", transactionView);

        // Assert
        assertEquals(transactionId.transactionId().toString(), response.id());
        assertEquals("account-123", response.accountId());
        assertEquals("AAPL", response.symbol());
        assertEquals(BigDecimal.valueOf(15).setScale(Precision.getMoneyPrecision()), response.fee()); // 5 + 10 from mocked fees
    }

    @Test
    @DisplayName("toResponse handles null fees")
    void toResponse_handlesNullFees() {
        TransactionId transactionId = TransactionId.randomId();
        Money price = Money.of(BigDecimal.valueOf(100), "USD");

        TransactionView transactionView = new TransactionView(
                transactionId,
                TransactionType.BUY,
                "AAPL",
                BigDecimal.TEN,
                price,
                null, // null fees
                Money.of(BigDecimal.valueOf(1000), "USD"),
                Instant.now(),
                "Test note");

        var response = mapper.toResponse("account-123", transactionView);

        assertEquals(BigDecimal.ZERO, response.fee());
    }

    @Test
    @DisplayName("toPagedResponse maps TransactionHistoryView to PagedTransactionHttpResponse correctly")
    void toPagedResponse_mapsCorrectly() {
        TransactionView tx1 = new TransactionView(
                TransactionId.randomId(),
                TransactionType.BUY,
                "AAPL",
                BigDecimal.TEN,
                Money.of(BigDecimal.valueOf(100), "USD"),
                List.of(),
                Money.of(BigDecimal.valueOf(1000), "USD"),
                Instant.now(),
                "Note 1");
        TransactionView tx2 = new TransactionView(
                TransactionId.randomId(),
                TransactionType.SELL,
                "GOOG",
                BigDecimal.valueOf(5),
                Money.of(BigDecimal.valueOf(200), "USD"),
                List.of(),
                Money.of(BigDecimal.valueOf(1000), "USD"),
                Instant.now(),
                "Note 2");

        TransactionHistoryView history = new TransactionHistoryView(
                List.of(tx1, tx2),
                1,
                20,
                2,
                1,
                false,
                false,
                new DateRangeView(Instant.now().minusSeconds(3600), Instant.now()));

        PagedTransactionHttpResponse response = mapper.toPagedResponse("account-123", history);

        assertEquals(2, response.transactions().size());
        assertEquals("account-123", response.transactions().get(0).accountId());
        assertEquals(tx1.transactionId().transactionId().toString(), response.transactions().get(0).id());
        assertEquals(tx2.transactionId().transactionId().toString(), response.transactions().get(1).id());

        assertEquals(20, response.meta().pageNumber());
        assertEquals(2, response.meta().pageSize());
        assertEquals(1, response.meta().totalElements());
        assertEquals(1, response.meta().totalPages());

        assertNotNull(response.dateRange());
        assertNotNull(response.transactions().get(0).recordedAt()); // timestamp mapping
    }
}
