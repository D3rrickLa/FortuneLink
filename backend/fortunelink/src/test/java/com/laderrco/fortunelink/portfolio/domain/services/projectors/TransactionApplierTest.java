package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.factories.TransactionFactory;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TaxLot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("TransactionApplier Logic Unit Tests")
class TransactionApplierTest {
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private final AssetType TYPE = AssetType.STOCK;
  private final Currency CAD = Currency.CAD;
  private final Instant T1 = Instant.parse("2023-01-01T10:00:00Z");

  private Position setupPosition() {
    return new FifoPosition(SYMBOL, TYPE, CAD, List.of(lot("10", "100", T1)), T1);
  }

  private TaxLot lot(String qty, String basis, Instant date) {
    return new TaxLot(new Quantity(new BigDecimal(qty)), new Money(new BigDecimal(basis), CAD),
        date);
  }

  @Nested
  @DisplayName("BUY and SELL Transactions")
  class TradeTests {
    @Test
    @DisplayName("apply: success on mapping buy to position buy")
    void applyMapsBuyToPositionBuy() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.buyBuilder(Quantity.of(5), Price.of("10", CAD)).build();

      var result = TransactionApplier.apply(pos, tx);

      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("apply: success on mapping sell to position sell")
    void applyMapsSellToPositionSell() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.baseBuilder().transactionType(TransactionType.SELL)
          .execution(new TradeExecution(SYMBOL, Quantity.of(5), Price.of("10", CAD)))
          .cashDelta(Money.of("50", CAD)).build();

      var result = TransactionApplier.apply(pos, tx);

      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("apply: fail on insufficient quantity for sell")
    void applyThrowsOnInsufficientQuantity() {
      Position emptyPos = AcbPosition.empty(SYMBOL, TYPE, CAD);
      Transaction tx = TransactionFactory.sellBuilder(Quantity.of(999), Price.of("1.00", CAD))
          .build();

      assertThatThrownBy(() -> TransactionApplier.apply(emptyPos, tx)).isInstanceOf(
          RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("Corporate Action Transactions")
  class CorporateActionTests {
    @Test
    @DisplayName("apply: success on mapping split to position split")
    void applyMapsSplitToPositionSplit() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.baseBuilder().transactionType(TransactionType.SPLIT)
          .split(new Ratio(2, 1))
          .execution(new TradeExecution(SYMBOL, Quantity.of(10), Price.of("1", CAD))).build();

      var result = TransactionApplier.apply(pos, tx);

      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("apply: success on mapping ROC to position basis reduction")
    void applyMapsReturnOfCapitalToPositionApplyRoc() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.baseBuilder()
          .transactionType(TransactionType.RETURN_OF_CAPITAL)
          .execution(new TradeExecution(SYMBOL, Quantity.of(10), Price.of("1", CAD)))
          .cashDelta(Money.of("10.00", CAD)).build();

      var result = TransactionApplier.apply(pos, tx);

      assertThat(result.getUpdatedPosition().totalCostBasis().amount()).isEqualByComparingTo("90");
    }
  }

  @Nested
  @DisplayName("Dividend and Reinvestment")
  class DividendTests {
    static Stream<Arguments> accumulationProvider() {
      return Stream.of(Arguments.of(TransactionType.BUY,
              TransactionFactory.buyBuilder(Quantity.of(5), Price.of("10", Currency.CAD)).build()),
          Arguments.of(TransactionType.DIVIDEND_REINVEST,
              TransactionFactory.baseBuilder().transactionType(TransactionType.DIVIDEND_REINVEST)
                  .execution(new TradeExecution(new AssetSymbol("AAPL"), Quantity.of(5),
                      Price.of("10", Currency.CAD))).build()));
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("accumulationProvider")
    @DisplayName("apply: success on mapping accumulation types to position buy")
    void applyMapsAccumulationTransactionsToPositionBuy(TransactionType type, Transaction tx) {
      Position pos = setupPosition();

      var result = TransactionApplier.apply(pos, tx);

      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("15");
    }
  }

  @Nested
  @DisplayName("Default and Unhandled Transactions")
  class DefaultTests {
    @Test
    @DisplayName("apply: success on mapping unknown type to no change")
    void applyMapsUnknownTypeToNoChangeResult() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.baseBuilder()
          .transactionType(TransactionType.TRANSFER_OUT).build();

      var result = TransactionApplier.apply(pos, tx);

      assertThat(result).isInstanceOf(ApplyResult.NoChange.class);
    }
  }
}