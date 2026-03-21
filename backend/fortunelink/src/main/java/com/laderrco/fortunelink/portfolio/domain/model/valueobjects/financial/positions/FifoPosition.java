package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TaxLot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record FifoPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency,
    List<TaxLot> lots, Instant lastModifiedAt) implements Position {
  public FifoPosition {
    notNull(symbol, "AssetSymbol");
    notNull(type, "type");
    notNull(accountCurrency, "accountCurrency");
    lots = lots == null ? List.of() : List.copyOf(lots);
  }

  public static FifoPosition empty(AssetSymbol symbol, AssetType type, Currency accountCurrency) {
    return new FifoPosition(symbol, type, accountCurrency, List.of(), null);
  }

  @Override
  public ApplyResult.Purchase<FifoPosition> buy(Quantity quantity, Money totalCost, Instant at) {
    TaxLot newLot = new TaxLot(quantity, totalCost, at);
    List<TaxLot> updatedLots = new ArrayList<>(lots);
    updatedLots.add(newLot);

    return new ApplyResult.Purchase<>(
        new FifoPosition(symbol, type, accountCurrency, updatedLots, at));
  }

  @Override
  public ApplyResult.Sale<FifoPosition> sell(Quantity quantity, Money proceeds, Instant at) {
    Quantity remainingToSell = quantity;
    Money costBasisSold = Money.zero(accountCurrency);
    List<TaxLot> remainingLots = new ArrayList<>();

    for (TaxLot lot : lots) {
      if (remainingToSell.isZero()) {
        remainingLots.add(lot);
        continue;
      }

      if (lot.quantity().compareTo(remainingToSell) <= 0) {
        // Consume entire lot
        costBasisSold = costBasisSold.add(lot.costBasis());
        remainingToSell = remainingToSell.subtract(lot.quantity());
      } else {
        Money consumedCost = lot.proportionalCost(remainingToSell);
        costBasisSold = costBasisSold.add(consumedCost);
        remainingLots.add(lot.remainingAfter(remainingToSell));
        remainingToSell = Quantity.ZERO;
      }
    }

    Money realizedGainLoss = proceeds.subtract(costBasisSold);

    return new ApplyResult.Sale<>(
        new FifoPosition(symbol, type, accountCurrency, remainingLots, at), costBasisSold,
        realizedGainLoss);
  }

  @Override
  public ApplyResult.Adjustment<FifoPosition> split(Ratio ratio) {
    List<TaxLot> splitLots = lots.stream().map(lot -> lot.split(ratio)).toList();

    return new ApplyResult.Adjustment<>(
        new FifoPosition(symbol, type, accountCurrency, splitLots, Instant.now()));
  }

  @Override
  public ApplyResult<FifoPosition> applyReturnOfCapital(Price price, Quantity heldQuantity) {
    if (!heldQuantity.equals(totalQuantity())) {
      throw new IllegalArgumentException(
          "ROC heldQuantity " + heldQuantity + " does not match position quantity "
              + totalQuantity());
    }

    Money totalReduction = price.calculateValue(heldQuantity);
    Money totalCostBasis = totalCostBasis();

    // Case 1: Cost basis already zero; entire ROC is an excess capital gain
    if (totalCostBasis.isZero()) {
      return new ApplyResult.RocAdjustment<>(this, totalReduction);
    }

    // Case 2: Full or excess wipeout; zero all lots
    // and return excess as capital gain
    if (totalReduction.isAtLeast(totalCostBasis)) {
      Money excessGain = totalReduction.subtract(totalCostBasis);
      List<TaxLot> zeroedLots = lots.stream()
          .map(l -> new TaxLot(l.quantity(), Money.zero(accountCurrency), l.acquiredDate()))
          .toList();

      FifoPosition updated = new FifoPosition(symbol, type, accountCurrency, zeroedLots,
          Instant.now());
      return new ApplyResult.RocAdjustment<>(updated, excessGain);
    }

    // Case 3: Partial reduction; distribute proportionally across lots
    Money remainingReduction = totalReduction;
    List<TaxLot> newLots = new ArrayList<>();

    for (int i = 0; i < lots.size(); i++) {
      TaxLot lot = lots.get(i);
      boolean isLastLot = (i == lots.size() - 1);

      Money lotReduction;
      if (isLastLot) {
        lotReduction = remainingReduction; // absorbs accumulated rounding drift
      } else {
        BigDecimal ratio = lot.costBasis().amount()
            .divide(totalCostBasis.amount(),
                Precision.DIVISION.getDecimalPlaces(),
                Rounding.DIVISION.getMode());

        lotReduction = totalReduction.multiply(ratio);
        remainingReduction = remainingReduction.subtract(lotReduction);
        // NOTE: lotReduction cannot exceed lot.costBasis() here.
        // Proven by: totalReduction < totalCostBasis (Case 2 guard above)
        // therefore: totalReduction * (lot.costBasis/totalCostBasis) < lot.costBasis
      }

      Money newCostBasis = applyLotReduction(lot.costBasis(), lotReduction, isLastLot, accountCurrency);
      newLots.add(new TaxLot(lot.quantity(), newCostBasis, lot.acquiredDate()));
    }

    return new ApplyResult.Adjustment<>(
        new FifoPosition(symbol, type, accountCurrency, newLots, Instant.now()));
  }

  @Override
  public Quantity totalQuantity() {
    return lots.stream().map(TaxLot::quantity).reduce(Quantity.ZERO, Quantity::add);
  }

  @Override
  public Money totalCostBasis() {
    return lots.stream().map(TaxLot::costBasis).reduce(Money.zero(accountCurrency), Money::add);
  }

  @Override
  public Money costPerUnit() {
    return isEmpty() ? Money.zero(accountCurrency) : totalCostBasis().divide(totalQuantity());
  }

  @Override
  public Money currentValue(Price currentPrice) {
    return currentPrice.calculateValue(totalQuantity());
  }

  static Money applyLotReduction(Money lotBasis, Money lotReduction, boolean isLastLot, Currency currency) {
    Money newCostBasis = lotBasis.subtract(lotReduction);

    if (newCostBasis.isNegative()) {
      if (isLastLot) {
        return Money.zero(currency);
      } else {
        throw new IllegalStateException("Intermediate lot went negative -> lotBasis: " + lotBasis +
            ", lotReduction: " + lotReduction);
      }
    }
    return newCostBasis;
  }
}
