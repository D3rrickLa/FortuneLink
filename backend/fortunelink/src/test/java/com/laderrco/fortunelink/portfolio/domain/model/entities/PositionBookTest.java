package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class PositionBookTest {
  private static final Currency USD = Currency.USD;

  @Test
  void applyResultThrowsExceptionWhenSymbolsDoNotMatch() {
    AssetSymbol symbol = new AssetSymbol("AAPL");
    AcbPosition acb = AcbPosition.empty(new AssetSymbol("MSFT"), AssetType.STOCK, USD);

    PositionBook pos = new PositionBook(USD, PositionStrategy.ACB);

    assertThatThrownBy(() -> pos.applyResult(symbol, acb)).isInstanceOf(
        IllegalArgumentException.class);
  }

  @ParameterizedTest
  @EnumSource(value = PositionStrategy.class, names = { "LIFO", "SPECIFIC_ID" })
  void ensurePositionThrowExceptionWhenStrategyIsNotSupported(
      PositionStrategy unsupportedStrategy) {
    AssetSymbol symbol = new AssetSymbol("AAPL");
    AssetType type = AssetType.STOCK;

    PositionBook posBook = new PositionBook(USD, unsupportedStrategy);

    assertThatThrownBy(() -> posBook.ensurePosition(symbol, type)).isInstanceOf(
        IllegalArgumentException.class)
        .hasMessageContaining(unsupportedStrategy.name() + " not supported");
  }

  @Test
  @DisplayName("applyResult: removes position from book when quantity reaches zero")
  void shouldRemovePositionWhenQuantityIsZero() {
    
    AssetSymbol symbol = new AssetSymbol("AAPL");
    Currency cad = Currency.of("CAD");
    PositionBook book = new PositionBook(cad, PositionStrategy.ACB);

    
    AcbPosition initialPos = AcbPosition.empty(symbol, AssetType.STOCK, cad)
        .buy(Quantity.of(10), Money.of("1500", cad), Instant.now())
        .newPosition();

    book.applyResult(symbol, initialPos);
    assertThat(book.has(symbol)).isTrue(); 

    
    
    AcbPosition closedPos = initialPos
        .sell(Quantity.of(10), Money.of("160", cad), Instant.now())
        .newPosition();

    assertThat(closedPos.totalQuantity().isZero()).isTrue();

    book.applyResult(symbol, closedPos);

    
    assertThat(book.has(symbol)).isFalse();
    assertThat(book.get(symbol)).isEmpty();
    assertThat(book.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("applyResult: updates position when quantity remains non-zero")
  void shouldUpdatePositionWhenQuantityIsStillPositive() {
    AssetSymbol symbol = new AssetSymbol("AAPL");
    Currency cad = Currency.of("CAD");
    PositionBook book = new PositionBook(cad, PositionStrategy.ACB);

    
    AcbPosition initialPos = AcbPosition.empty(symbol, AssetType.STOCK, cad)
        .buy(Quantity.of(10), Money.of("100", cad), Instant.now())
        .newPosition();

    AcbPosition updatedPos = initialPos
        .sell(Quantity.of(5), Money.of("110", cad), Instant.now())
        .newPosition();

    book.applyResult(symbol, updatedPos);

    
    assertThat(book.has(symbol)).isTrue();
    assertThat(book.get(symbol).get().totalQuantity().amount())
        .isEqualByComparingTo("5");
  }
}
