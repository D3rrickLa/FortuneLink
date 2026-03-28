package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public class PositionBookTest {
  private static final Currency USD = Currency.USD;

  @Test
  void applyResultThrowsExceptionWhenSymbolsDoNotMatch() {
    AssetSymbol symbol = new AssetSymbol("AAPL");
    AcbPosition acb = AcbPosition.empty(new AssetSymbol("MSFT"), AssetType.STOCK, USD);

    PositionBook pos = new PositionBook(USD, PositionStrategy.ACB);

    assertThatThrownBy(() -> pos.applyResult(symbol, acb))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @EnumSource(value = PositionStrategy.class, names = { "LIFO", "SPECIFIC_ID" })
  void ensurePosition_ShouldThrowException_WhenStrategyIsNotSupported(PositionStrategy unsupportedStrategy) {
    AssetSymbol symbol = new AssetSymbol("AAPL");
    AssetType type = AssetType.STOCK;

    PositionBook posBook = new PositionBook(USD, unsupportedStrategy);

    assertThatThrownBy(() -> posBook.ensurePosition(symbol, type))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(unsupportedStrategy.name() + " not supported");
  }
}
