package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import static org.assertj.core.api.Assertions.assertThat;
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
@DisplayName("FifoPositionProjector: Projection of Transaction Streams")
public class FifoPositionProjectorTest {
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private final AssetType TYPE = AssetType.STOCK;
  private final Currency CAD = Currency.CAD;

  private final FifoPositionProjector projector = new FifoPositionProjector(SYMBOL, TYPE, CAD);

  @Test
  @DisplayName("project: successfully accumulates fifo position state")
  void projectAccumulatesPositionStateCorrectly() {

    Transaction buy = TransactionFactory.buyBuilder(Quantity.of(10), Price.of("10", CAD))
        .occurredAt(Instant.parse("2023-01-01T10:00:00Z")).build();

    Transaction sell = TransactionFactory.sellBuilder(Quantity.of(5), Price.of("15", CAD))
        .occurredAt(Instant.parse("2023-01-02T10:00:00Z")).build();

    FifoPosition result = projector.project(List.of(sell, buy));

    assertThat(result.totalQuantity().amount()).isEqualByComparingTo("5");
  }

  @Test
  @DisplayName("project: sorts deterministically by ID when timestamps are identical")
  void projectSortsByIdWhenTimestampsMatch() {
    Instant sameTime = Instant.parse("2023-01-01T10:00:00Z");
    Transaction txA = TransactionFactory.buyBuilder(Quantity.of(10), Price.of("10", CAD))
        .transactionId(TransactionId.newId()).occurredAt(sameTime).build();

    Transaction txB = TransactionFactory.sellBuilder(Quantity.of(5), Price.of("15", CAD))
        .transactionId(TransactionId.newId()).occurredAt(sameTime).build();

    FifoPosition result = projector.project(List.of(txB, txA));
    assertThat(result.totalQuantity().amount()).isEqualByComparingTo("5");
  }

  @Test
  @DisplayName("project: throws checkInstance method exception in base class")
  void projectThrowsExceptionWhenTransactionApplierReturnsWrongPositionType() {
    AssetSymbol symbol = new AssetSymbol("AAPL");
    AssetType type = AssetType.STOCK;
    Currency currency = Currency.USD;

    FifoPositionProjector projector = new FifoPositionProjector(symbol, type, currency);

    try (MockedStatic<TransactionApplier> utilities = mockStatic(TransactionApplier.class)) {

      AcbPosition wrongPosition = mock(AcbPosition.class);

      ApplyResult<Position> resultWithWrongType = new ApplyResult.Adjustment<>(wrongPosition);

      utilities.when(() -> TransactionApplier.apply(any(), any())).thenReturn(resultWithWrongType);

      assertThrows(IllegalStateException.class, () -> {
        projector.project(List.of(mock(Transaction.class)));
      });
    }
  }
}