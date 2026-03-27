package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.factories.TransactionFactory;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AcbPositionProjector: Projection of Transaction Streams")
class AcbPositionProjectorTest {

  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private final AssetType TYPE = AssetType.STOCK;
  private final Currency CAD = Currency.CAD;

  private final AcbPositionProjector projector = new AcbPositionProjector(SYMBOL, TYPE, CAD);

  @Test
  @DisplayName("project: successfully accumulates acb position state")
  void projectAccumulatesPositionStateCorrectly() {
    // 1. Arrange: Create a stream of events
    Transaction buy = TransactionFactory.buyBuilder(Quantity.of(10), Price.of("10", CAD))
        .occurredAt(Instant.parse("2023-01-01T10:00:00Z")).build();

    Transaction sell = TransactionFactory.sellBuilder(Quantity.of(5), Price.of("15", CAD))
        .occurredAt(Instant.parse("2023-01-02T10:00:00Z")).build();

    // 2. Act: Project the stream
    AcbPosition result = projector.project(List.of(sell, buy)); // Out of order list

    // 3. Assert: Final state
    assertThat(result.totalQuantity().amount()).isEqualByComparingTo("5");
  }

  @Test
  @DisplayName("project: failure when there is type mismatch")
  @Disabled("Unreachable via normal construction -> AcbPosition methods always return AcbPosition."
      + " Guard exists to catch future Position hierarchy changes or TransactionApplier regressions.")
  void testProject_fail_throwsOnTypeMismatch() {
    // To trigger: TransactionApplier.apply() would need to return a FifoPosition
    // from an AcbPosition input, which is structurally impossible given the
    // sealed interface + concrete implementations.
    // If this guard ever fires in production, the Position type hierarchy changed
    // without updating this projector.
  }
}