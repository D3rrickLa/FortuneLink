package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;

public record AcbPosition(
    AssetSymbol symbol,
    AssetType type,
    Currency accountCurrency,
    Quantity totalQuantity,
    Money totalCostBasis,
    Instant firstAcquiredAt,
    Instant lastModifiedAt) implements Position {
  public AcbPosition {
    notNull(symbol, "AssetSymbol");
    notNull(type, "type");
    notNull(accountCurrency, "accountCurrency");
    notNull(totalQuantity, "totalQuantity");
    notNull(totalCostBasis, "totalCostBasis");
  }

  public static AcbPosition empty(AssetSymbol symbol, AssetType type, Currency currency) {
    return new AcbPosition(symbol, type, currency, Quantity.ZERO, Money.zero(currency), null, null);
  }

  @Override
  public ApplyResult.Purchase<AcbPosition> buy(Quantity quantity, Money totalCost, Instant at) {
    Instant newAcquiredDate = (this.totalQuantity.isZero()) ? at : this.firstAcquiredAt;

    // totalQuantity.add() -> accumulate quantity
    // totalCostBasis.add() -> net price + commission
    AcbPosition updated = new AcbPosition(symbol, type, accountCurrency,
        totalQuantity.add(quantity), totalCostBasis.add(totalCost), newAcquiredDate, at);

    return new ApplyResult.Purchase<>(updated);
  }

  @Override
  public ApplyResult.Sale<AcbPosition> sell(Quantity quantity, Money proceeds, Instant at) {
    // Domain invariant: cannot sell from an empty position
    if (totalQuantity.isZero()) {
        throw new IllegalStateException(
            String.format("Cannot sell %s of %s: position is empty", quantity.amount(), symbol.symbol())
        );
    }

    // Domain invariant: cannot sell more than held
    if (quantity.compareTo(totalQuantity) > 0) {
        throw new IllegalStateException(
            String.format("Cannot sell %s of %s: only %s held",
                quantity.amount(), symbol.symbol(), totalQuantity.amount())
        );
    }

    boolean isFullLiquidation = quantity.equals(totalQuantity);
    Money costBasisSold = isFullLiquidation
        ? totalCostBasis
        : totalCostBasis.multiply(
            quantity.amount().divide(totalQuantity.amount(),
                Precision.DIVISION.getDecimalPlaces(),
                Rounding.DIVISION.getMode()));

    Money newCostBasis = isFullLiquidation
        ? Money.zero(accountCurrency)
        : totalCostBasis.subtract(costBasisSold);

    Money realizedGain = proceeds.subtract(costBasisSold);

    AcbPosition updated = new AcbPosition(
        symbol, type, accountCurrency,
        totalQuantity.subtract(quantity),
        newCostBasis, firstAcquiredAt, at);

    return new ApplyResult.Sale<>(updated, costBasisSold, realizedGain);
  }

  @Override
  public ApplyResult.Adjustment<AcbPosition> split(Ratio ratio) {
    // Use the Ratio to calculate the new quantity precisely
    Quantity newQuantity = this.totalQuantity.multiply(BigDecimal.valueOf(ratio.numerator()))
        .divide(BigDecimal.valueOf(ratio.denominator()));

    // Cost basis doesn't change in a split
    AcbPosition updated = new AcbPosition(symbol, type, accountCurrency, newQuantity,
        totalCostBasis, firstAcquiredAt, Instant.now());
    return new ApplyResult.Adjustment<>(updated);
  }

  @Override
  public ApplyResult<AcbPosition> applyReturnOfCapital(Price price, Quantity heldQuantity) {
    if (!heldQuantity.equals(this.totalQuantity)) {
      throw new IllegalArgumentException(
          "ROC heldQuantity " + heldQuantity + " does not match position quantity "
              + totalQuantity);
    }

    Money totalReduction = price.calculateValue(heldQuantity);

    Money newCostBasis;
    Money excessCapitalGain;

    if (totalReduction.isAtLeast(totalCostBasis)) {
      excessCapitalGain = totalReduction.subtract(totalCostBasis);
      newCostBasis = Money.zero(accountCurrency);
    } else {
      excessCapitalGain = Money.zero(accountCurrency);
      newCostBasis = totalCostBasis.subtract(totalReduction);
    }

    AcbPosition updated = new AcbPosition(symbol, type, accountCurrency, totalQuantity,
        newCostBasis, firstAcquiredAt, Instant.now());

    if (excessCapitalGain.isPositive()) {
      return new ApplyResult.RocAdjustment<>(updated, excessCapitalGain);
    }

    return new ApplyResult.Adjustment<>(updated);
  }

  @Override
  public Money costPerUnit() {
    return isEmpty() ? Money.zero(accountCurrency) : totalCostBasis.divide(totalQuantity.amount());
  }

  @Override
  public Money currentValue(Price currentPrice) {
    return currentPrice.calculateValue(totalQuantity);

  }

  public Money calculateUnrealizedGain(Price currentPrice) {
    return currentValue(currentPrice).subtract(totalCostBasis);
  }
}
