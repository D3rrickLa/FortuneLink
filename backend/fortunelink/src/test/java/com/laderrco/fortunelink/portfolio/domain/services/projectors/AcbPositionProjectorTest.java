package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.factories.TransactionFactory;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcbPositionProjector: Projection of Transaction Streams")
class AcbPositionProjectorTest {

  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private final AssetType TYPE = AssetType.STOCK;
  private final Currency CAD = Currency.CAD;

  private final AcbPositionProjector projector = new AcbPositionProjector(SYMBOL, TYPE, CAD);

  @Test
  @DisplayName("project: successfully accumulates acb position state")
  void projectAccumulatesPositionStateCorrectly() {
    Transaction buy = TransactionFactory.buyBuilder(Quantity.of(10), Price.of("10", CAD))
        .occurredAt(Instant.parse("2023-01-01T10:00:00Z")).build();

    Transaction sell = TransactionFactory.sellBuilder(Quantity.of(5), Price.of("15", CAD))
        .occurredAt(Instant.parse("2023-01-02T10:00:00Z")).build();

    AcbPosition result = projector.project(List.of(sell, buy)); // Out of order list

    assertThat(result.totalQuantity().amount()).isEqualByComparingTo("5");
  }

  @Test
  @DisplayName("project: sorts deterministically by ID when timestamps are identical")
  void projectSortsByIdWhenTimestampsMatch() {
    Instant sameTime = Instant.parse("2023-01-01T10:00:00Z");
    TransactionId buyId = TransactionId.fromString("63fdb51b-dfa6-47d6-93ea-6142e7a02d5d");
    TransactionId buyId2 = TransactionId.fromString("63fdb51b-dfa6-47d6-93ea-6142e7a02d5c");
    Transaction txA = TransactionFactory.buyBuilder(Quantity.of(10), Price.of("10", CAD))
        .transactionId(buyId)
        .occurredAt(sameTime)
        .build();

    Transaction txB = TransactionFactory.buyBuilder(Quantity.of(5), Price.of("15", CAD))
        .transactionId(buyId2)
        .occurredAt(sameTime)
        .build();

    AcbPosition result = projector.project(List.of(txB, txA));
    assertThat(result.totalQuantity().amount()).isEqualByComparingTo("15");
  }

  @Test
  @DisplayName("project: prioritizes BUY over SELL when timestamps are identical")
  void projectPrioritizesBuyOverSell() {
    Instant sameTime = Instant.parse("2023-01-01T10:00:00Z");

    // Ensure the Sell ID is "smaller" than the Buy ID to force the tie-breaker
    TransactionId buyId = TransactionId.fromString("63fdb51b-dfa6-47d6-93ea-6142e7a02d5d");
    TransactionId sellId = TransactionId.fromString("63fdb51b-dfa6-47d6-93ea-6142e7a02d5c");

    Transaction buy = TransactionFactory.buyBuilder(Quantity.of(20), Price.of("30", CAD))
        .transactionId(buyId).occurredAt(sameTime).build();
    Transaction sell = TransactionFactory.sellBuilder(Quantity.of(10), Price.of("60", CAD))
        .transactionId(sellId).occurredAt(sameTime).build();

    // This should now pass because the comparator moves 'buy' to the front
    assertDoesNotThrow(() -> projector.project(List.of(sell, buy)));
  }

  @Test
  @DisplayName("project: throws checkInstance method exception in base class")
  void projectThrowsExceptionWhenTransactionApplierReturnsWrongPositionType() {
    AssetSymbol symbol = new AssetSymbol("AAPL");
    AssetType type = AssetType.STOCK;
    Currency currency = Currency.USD;

    AcbPositionProjector projector = new AcbPositionProjector(symbol, type, currency);

    try (MockedStatic<TransactionApplier> utilities = mockStatic(TransactionApplier.class)) {
      FifoPosition wrongPosition = mock(FifoPosition.class);

      ApplyResult<Position> resultWithWrongType = new ApplyResult.Adjustment<>(wrongPosition);

      utilities.when(() -> TransactionApplier.apply(any(), any())).thenReturn(resultWithWrongType);

      assertThrows(IllegalStateException.class, () -> {
        projector.project(List.of(mock(Transaction.class)));
      });
    }
  }
}