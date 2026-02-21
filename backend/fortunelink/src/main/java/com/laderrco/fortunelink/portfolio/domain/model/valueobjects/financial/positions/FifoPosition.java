package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TaxLot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public final record FifoPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency, List<TaxLot> lots) implements Position {

    public FifoPosition {
        notNull(symbol, "AssetSymbol");
        notNull(type, "type");
        notNull(accountCurrency, "accountCurrency");
        lots = lots == null ? List.of() : List.copyOf(lots);
    }

    public static FifoPosition empty(AssetSymbol symbol, AssetType type, Currency accountCurrency) {
        return new FifoPosition(symbol, type, accountCurrency, List.of());
    }

    @Override
    public ApplyResult<FifoPosition> buy(
            Quantity quantity,
            Money totalCost,
            Instant at) {
        TaxLot newLot = new TaxLot(quantity, totalCost, at);

        List<TaxLot> updatedLots = new ArrayList<>(lots);
        updatedLots.add(newLot);

        return new ApplyResult.Purchase<>(new FifoPosition(symbol, type, accountCurrency, updatedLots));
    }

    @Override
    public ApplyResult<FifoPosition> sell(
            Quantity quantity,
            Money proceeds,
            Instant at) {
        if (!hasSufficientQuantity(quantity)) {
            throw new IllegalStateException("Insufficient quantity");
        }

        Quantity remainingToSell = quantity;
        Money costBasisSold = Money.ZERO(accountCurrency);
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
                // Partial lot consumption
                Money perUnitCost = lot.costBasis().divide(lot.quantity().amount());
                Money consumedCost = perUnitCost.multiply(remainingToSell.amount());

                costBasisSold = costBasisSold.add(consumedCost);

                Quantity remainingQty = lot.quantity().subtract(remainingToSell);
                Money remainingCost = lot.costBasis().subtract(consumedCost);

                remainingLots.add(new TaxLot(remainingQty, remainingCost, lot.acquiredDate()));

                remainingToSell = Quantity.ZERO;
            }
        }

        Money realizedGainLoss = proceeds.subtract(costBasisSold);

        return new ApplyResult.Sale<>(
                new FifoPosition(symbol, type, accountCurrency, remainingLots),
                costBasisSold,
                realizedGainLoss);
    }

    @Override
    public ApplyResult<FifoPosition> split(double ratio) {
        if (ratio <= 0) {
            throw new IllegalArgumentException("Split ratio must be positive");
        }

        List<TaxLot> splitLots = lots.stream()
                .map(lot -> lot.split(ratio))
                .toList();

        return new ApplyResult.NoChange<>(
                new FifoPosition(symbol, type, accountCurrency, splitLots));
    }

    @Override
    public Quantity totalQuantity() {
        return lots.stream()
                .map(TaxLot::quantity)
                .reduce(Quantity.ZERO, Quantity::add);
    }

    @Override
    public Money totalCostBasis() {
        return lots.stream()
                .map(TaxLot::costBasis)
                .reduce(Money.ZERO(accountCurrency), Money::add);
    }

    @Override
    public Money costPerUnit() {
        return isEmpty() ? Money.ZERO(accountCurrency)
                : totalCostBasis().divide(totalQuantity());
    }

    @Override
    public Money currentValue(Money currentPrice) {
        return currentPrice.multiply(totalQuantity());
    }

}
