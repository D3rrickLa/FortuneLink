package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.SplitDetails;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TransactionDate;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
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
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

public class TransactionApplierTest {
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private final AssetType TYPE = AssetType.STOCK;
  private final Currency CAD = Currency.CAD;
  private final Instant T1 = Instant.parse("2023-01-01T10:00:00Z");
  private final Instant T2 = Instant.parse("2023-02-01T10:00:00Z");

  @Nested
  @DisplayName("BUY/SELL Transactions")
  class TradeTests {
    @Test
    @DisplayName("apply_success_mapsBuyTransactionToPositionBuy")
    void apply_success_mapsBuyTransactionToPositionBuy() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory
          .buyBuilder(Quantity.of(5), Price.of("10", CAD))
          .build();

      var result = TransactionApplier.apply(pos, tx);
      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("apply_success_mapsSellTransactionToPositionSell")
    void apply_success_mapsSellTransactionToPositionSell() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.baseBuilder()
          .transactionType(TransactionType.SELL)
          .execution(new TradeExecution(SYMBOL, Quantity.of(5), Price.of("10", CAD)))
          .cashDelta(Money.of("50", CAD))
          .build();

      var result = TransactionApplier.apply(pos, tx);
      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("apply_fail_throwsOnInsufficientQuantityForSell")
    void apply_fail_throwsOnInsufficientQuantityForSell() {
      Position pos = AcbPosition.empty(SYMBOL, TYPE, CAD);
      Transaction tx = TransactionFactory
          .sellBuilder(Quantity.of(999), Price.of("1.00", CAD))
          .build();

      assertThatThrownBy(() -> TransactionApplier.apply(pos, tx))
          .isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("Corporate Action Transactions")
  class CorporateActionTests {
    @Test
    @DisplayName("apply_success_mapsSplitTransactionToPositionSplit")
    void apply_success_mapsSplitTransactionToPositionSplit() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.baseBuilder()
          .transactionType(TransactionType.SPLIT)
          .split(new SplitDetails(new Ratio(2, 1)))
          .execution(new TradeExecution(SYMBOL, Quantity.of(10), Price.of("1", CAD)))
          .build();

      var result = TransactionApplier.apply(pos, tx);
      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("apply_success_mapsReturnOfCapitalToPositionApplyRoc")
    void apply_success_mapsReturnOfCapitalToPositionApplyRoc() {
      Position pos = setupPosition();
      Money expectedCashDelta = Money.of("10.00", CAD);
      Transaction tx = TransactionFactory.baseBuilder()
          .transactionType(TransactionType.RETURN_OF_CAPITAL)
          .execution(new TradeExecution(SYMBOL, Quantity.of(10), Price.of("1", CAD)))
          .cashDelta(expectedCashDelta)
          .build();

      var result = TransactionApplier.apply(pos, tx);
      assertThat(result.getUpdatedPosition().totalCostBasis().amount()).isEqualByComparingTo("90");
    }
  }

  @Nested
  @DisplayName("Dividend/Reinvestment Transactions")
  class DividendTests {
    @Test
    @DisplayName("apply_success_mapsDividendReinvestToPositionBuy")
    void apply_success_mapsDividendReinvestToPositionBuy() {
      Position pos = setupPosition();
      // Maps to position.buy() internally
      Transaction tx = TransactionFactory.baseBuilder()
          .transactionType(TransactionType.DIVIDEND_REINVEST)
          .execution(new TradeExecution(SYMBOL, Quantity.of(5), Price.of("10", CAD)))
          .build();

      var result = TransactionApplier.apply(pos, tx);
      assertThat(result.getUpdatedPosition().totalQuantity().amount()).isEqualByComparingTo("15");
    }
  }

  @Nested
  @DisplayName("Default/Unhandled Transactions")
  class DefaultTests {
    @Test
    @DisplayName("apply_success_mapsUnknownTypeToNoChangeResult")
    void apply_success_mapsUnknownTypeToNoChangeResult() {
      Position pos = setupPosition();
      Transaction tx = TransactionFactory.baseBuilder()
          .transactionType(TransactionType.TRANSFER_OUT) // Or any type without a case
          .build();

      var result = TransactionApplier.apply(pos, tx);
      assertThat(result).isInstanceOf(ApplyResult.NoChange.class);
    }
  }

  private Position setupPosition() {
    return new FifoPosition(SYMBOL, TYPE, CAD, List.of(lot("10", "100", T1)), T1);
  }

  private TaxLot lot(String qty, String basis, Instant date) {
    return new TaxLot(new Quantity(new BigDecimal(qty)), new Money(new BigDecimal(basis), CAD), date);
  }

  class TransactionFactory {
    private final static Currency CAD = Currency.CAD;

    public static Transaction.TransactionBuilder baseBuilder() {
      return Transaction.builder()
          .transactionId(TransactionId.newId())
          .accountId(AccountId.newId())
          .cashDelta(Money.ZERO(CAD)) // Default to zero if not relevant
          .fees(List.of())
          .metadata(TransactionMetadata.manual(AssetType.STOCK))
          .occurredAt(TransactionDate.of(Instant.now()))
          .notes("");
    }

    public static Transaction.TransactionBuilder sellBuilder(Quantity q, Price p) {
      return baseBuilder()
          .transactionType(TransactionType.SELL)
          .execution(new TradeExecution(new AssetSymbol("AAPL"), q, p))
          .cashDelta(p.calculateValue(q)); // Correctly calculate the delta
    }

    public static Transaction.TransactionBuilder buyBuilder(Quantity q, Price p) {
      Money totalCost = p.calculateValue(q).negate();

      return baseBuilder()
          .transactionType(TransactionType.BUY)
          .execution(new TradeExecution(new AssetSymbol("AAPL"), q, p))
          .cashDelta(totalCost);
    }
  }
}
