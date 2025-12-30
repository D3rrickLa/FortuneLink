package com.laderrco.fortunelink.portfolio_management.application.mappers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionMapperTest {
    private ValidatedCurrency USD = ValidatedCurrency.USD;
    @Test
    @DisplayName("Should map Entity to Response with all fields correctly")
    void shouldMapEntityToResponse() {
        // Arrange
        TransactionId txId = TransactionId.randomId();
        AssetIdentifier asset = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
        Money price = new Money(new BigDecimal("150.00"), USD);
        Instant now = Instant.now();
        
        // Mocking the entity to avoid complex setup
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTransactionId()).thenReturn(txId);
        when(transaction.getTransactionType()).thenReturn(TransactionType.BUY);
        when(transaction.getAssetIdentifier()).thenReturn(asset);
        when(transaction.getQuantity()).thenReturn(new BigDecimal("10"));
        when(transaction.getPricePerUnit()).thenReturn(price);
        when(transaction.getFees()).thenReturn(List.of(new Fee(FeeType.ACCOUNT_MAINTENANCE, new Money(BigDecimal.ONE, USD), ExchangeRate.createSingle(USD, null), null, Instant.now())));
        when(transaction.calculateTotalCost()).thenReturn(new Money(new BigDecimal("1501.00"), USD));
        when(transaction.getTransactionDate()).thenReturn(now);
        when(transaction.getNotes()).thenReturn("Test note");

        // Act
        TransactionResponse response = TransactionMapper.toResponse(transaction, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.transactionId()).isEqualTo(txId);
        assertThat(response.symbol()).isEqualTo("AAPL");
        assertThat(response.totalCost().amount().setScale(2)).isEqualTo(new BigDecimal("1501.00"));
        assertThat(response.notes()).isEqualTo("Test note");
    }

    @Test
    @DisplayName("Should return null when mapping a null transaction")
    void shouldReturnNullForNullInput() {
        assertThat(TransactionMapper.toResponse(null, null)).isNull();
    }

    @Test
    @DisplayName("Should return empty list when input list is null or empty")
    void shouldReturnEmptyListForNullCollection() {
        assertThat(TransactionMapper.toResponseList(null)).isEmpty();
        assertThat(TransactionMapper.toResponseList(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Should map a list of transactions")
    void shouldMapList() {
        Transaction t1 = mock(Transaction.class);
        Transaction t2 = mock(Transaction.class);
        // Setup minimal returns to avoid NPEs in mapper
        when(t1.getTransactionId()).thenReturn(TransactionId.randomId());
        when(t1.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
        when(t1.getQuantity()).thenReturn(BigDecimal.ONE);
        when(t1.getAssetIdentifier()).thenReturn(new CashIdentifier("USD"));
        when(t1.getPricePerUnit()).thenReturn(Money.of(10, "USD"));
        when(t1.getFees()).thenReturn(List.of());
        when(t1.calculateTotalCost()).thenReturn(Money.of(20, "USD"));
        when(t1.getTransactionDate()).thenReturn(Instant.now());
        
        when(t2.getTransactionId()).thenReturn(TransactionId.randomId());
        when(t2.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
        when(t2.getQuantity()).thenReturn(BigDecimal.ONE);
        when(t2.getFees()).thenReturn(null);
        when(t2.getAssetIdentifier()).thenReturn(new CashIdentifier("CAD"));
        when(t2.getPricePerUnit()).thenReturn(Money.of(10, "USD"));
        when(t2.calculateTotalCost()).thenReturn(Money.of(20, "USD"));
        when(t2.getTransactionDate()).thenReturn(Instant.now());

        List<TransactionResponse> results = TransactionMapper.toResponseList(List.of(t1, t2));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).symbol()).isEqualTo("USD");
        assertThat(results.get(1).symbol()).isEqualTo("CAD");
    }
}
