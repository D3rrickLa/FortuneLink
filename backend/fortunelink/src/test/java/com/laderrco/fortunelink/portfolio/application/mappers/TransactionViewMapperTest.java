package com.laderrco.fortunelink.portfolio.application.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionViewMapper Unit Tests")
class TransactionViewMapperTest {

  // --- Constants ---
  private static final TransactionId TX_ID = TransactionId.newId();
  private static final Instant NOW = Instant.now();
  private static final String SYMBOL = "AAPL";
  private static final Currency USD = Currency.of("USD");
  @InjectMocks
  private TransactionViewMapper mapper;

  @Nested
  @DisplayName("toTransactionView Mapping")
  class SingleTransactionMapping {

    @Test
    @DisplayName("Should map cash transaction (null execution) correctly")
    void toTransactionView_WhenExecutionIsNull_ShouldMapBasicFields() {
      // Arrange
      Transaction tx = mock(Transaction.class);
      TransactionMetadata metadata = mock(TransactionMetadata.class);
      List<Fee> fees = List.of(mock(Fee.class));
      Money cashDelta = mock(Money.class);

      when(tx.transactionId()).thenReturn(TX_ID);
      when(tx.transactionType()).thenReturn(TransactionType.DEPOSIT);
      when(tx.execution()).thenReturn(null); // The branch trigger
      when(tx.fees()).thenReturn(fees);
      when(tx.cashDelta()).thenReturn(cashDelta);
      when(tx.metadata()).thenReturn(metadata);
      when(metadata.asFlatMap()).thenReturn(Map.of("source", "bank"));
      when(tx.occurredAt()).thenReturn(NOW);
      when(tx.notes()).thenReturn("Monthly savings");

      // Act
      TransactionView result = mapper.toTransactionView(tx);

      // Assert
      assertThat(result.transactionId()).isEqualTo(TX_ID);
      assertThat(result.symbol()).isNull();
      assertThat(result.quantity()).isNull();
      assertThat(result.price()).isNull();
      assertThat(result.notes()).isEqualTo("Monthly savings");
      assertThat(result.metadata()).containsEntry("source", "bank");
    }

    @Test
    @DisplayName("Should map asset transaction (execution exists) correctly")
    void toTransactionView_WhenExecutionExists_ShouldMapAssetDetails() {
      // Arrange
      Transaction tx = mock(Transaction.class);
      TradeExecution execution = mock(TradeExecution.class);
      AssetSymbol asset = mock(AssetSymbol.class);
      TransactionMetadata metadata = mock(TransactionMetadata.class);

      when(tx.transactionId()).thenReturn(TX_ID);
      when(tx.transactionType()).thenReturn(TransactionType.BUY);
      when(tx.execution()).thenReturn(execution);
      when(execution.asset()).thenReturn(asset);
      when(asset.symbol()).thenReturn(SYMBOL);
      when(execution.quantity()).thenReturn(new Quantity(BigDecimal.TEN));
      when(execution.pricePerUnit()).thenReturn(Price.of(BigDecimal.valueOf(150), USD));
      when(tx.metadata()).thenReturn(metadata);
      when(metadata.asFlatMap()).thenReturn(Collections.emptyMap());

      // Act
      TransactionView result = mapper.toTransactionView(tx);

      // Assert
      assertThat(result.symbol()).isEqualTo(SYMBOL);
      assertThat(result.quantity().amount()).isEqualByComparingTo("10");
      assertThat(result.price().amount()).isEqualByComparingTo("150");
    }
  }

  @Nested
  @DisplayName("toViewList Mapping")
  class ListMapping {

    @Test
    @DisplayName("Should return empty list when input is null or empty")
    void toViewList_ShouldHandleNullOrEmpty() {
      assertThat(mapper.toViewList(null)).isEmpty();
      assertThat(mapper.toViewList(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Should map all transactions in the list")
    void toViewList_ShouldMapAllItems() {
      // Arrange
      Transaction tx1 = mock(Transaction.class);
      Transaction tx2 = mock(Transaction.class);

      // Minimal mocking for both to avoid NPEs during mapping
      setupMinimalTx(tx1);
      setupMinimalTx(tx2);

      // Act
      List<TransactionView> results = mapper.toViewList(List.of(tx1, tx2));

      // Assert
      assertThat(results).hasSize(2);
      verify(tx1, times(1)).transactionId();
      verify(tx2, times(1)).transactionId();
    }

    private void setupMinimalTx(Transaction tx) {
      when(tx.metadata()).thenReturn(mock(TransactionMetadata.class));
      when(tx.execution()).thenReturn(null);
    }
  }
}