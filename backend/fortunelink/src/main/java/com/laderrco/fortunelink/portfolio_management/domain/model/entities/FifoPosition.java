package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.TaxLot;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

public final record FifoPosition(AssetSymbol assetSymbol, AssetType type, Currency accountCurrency, List<TaxLot> lots)
        implements Position, ClassValidation {

    public static FifoPosition empty(AssetSymbol symbol, AssetType type, Currency accountCurrency) {
        return new FifoPosition(symbol, type, accountCurrency, List.of());
    }

    public FifoPosition addPurchase(Quantity qty, Money costBasis, Instant acquiredDate) {
        if (qty.isZero()) {
            return this;
        }

        List<TaxLot> updated = new ArrayList<>(lots);
        updated.add(new TaxLot(qty, costBasis, acquiredDate));
        return new FifoPosition(assetSymbol, type, accountCurrency, updated);
    }

    public SaleResult reduceBySale(Quantity sellQty, Money saleProceeds) {
        if (sellQty.isZero()) {
            return SaleResult.noOp(this, accountCurrency);
        }

        Quantity remainingToSell = sellQty;
        Money realizedCost = Money.ZERO(accountCurrency);

        List<TaxLot> remainingLots = new ArrayList<>();
        List<TaxLot> consumedLots = new ArrayList<>();

        for (TaxLot lot : lots) {
            if (remainingToSell.isZero()) {
                remainingLots.add(lot);
                continue;
            }

            if (lot.quantity().compareTo(remainingToSell) <= 0) {
                consumedLots.add(lot);
                realizedCost = realizedCost.add(lot.costBasis());
                remainingToSell = remainingToSell.subtract(lot.quantity());
            } else {
                Money partialCost = lot.proportionalCost(remainingToSell);

                consumedLots.add(
                        new TaxLot(remainingToSell, partialCost, lot.acquiredDate()));

                remainingLots.add(lot.reduce(remainingToSell));
                realizedCost = realizedCost.add(partialCost);
                remainingToSell = Quantity.ZERO;
            }
        }

        if (!remainingToSell.isZero()) {
            throw new IllegalStateException(
                    "Attempted to sell more than available for " + assetSymbol);
        }

        Money realizedGainLoss = saleProceeds.subtract(realizedCost);

        return new SaleResult(
                new FifoPosition(assetSymbol, type, accountCurrency, remainingLots),
                realizedCost,
                consumedLots,
                realizedGainLoss);
    }

    public ApplyResult apply(Transaction tx) {

        if (tx.execution() == null || tx.execution().quantity().isZero()) {
            return new ApplyResult.NoChange(this);
        }

        Quantity delta = tx.execution().quantity();

        if (delta.isPositive()) {
            Money lotCost = tx.cashDelta().abs();

            return new ApplyResult.Purchase(
                    addPurchase(delta, lotCost, tx.occurredAt()));
        }

        SaleResult result = reduceBySale(
                delta.abs(),
                tx.cashDelta());

        return new ApplyResult.Sale(
                result.newPosition(),
                result.costBasisRealized(),
                result.lotsConsumed(),
                result.realizedGainLoss());
    }

    @Override
    public AssetSymbol symbol() {
        return assetSymbol;
    }

    @Override
    public AssetType type() {
        return type;
    }

    @Override
    public Currency accountCurrency() {
        return accountCurrency;
    }

    @Override
    public Quantity getTotalQuantity() {
        return lots().stream().map(TaxLot::quantity).reduce(Quantity.ZERO, Quantity::add);
    }

    @Override
    public Money getTotalCostBasis() {
        return lots.stream().map(TaxLot::costBasis).reduce(Money.ZERO(accountCurrency), Money::add);
    }

    @Override
    public Money calculateCurrentValue(Money currentPrice) {
        return currentPrice.multiply(getTotalQuantity().amount());
    }

    public record SaleResult(FifoPosition newPosition, Money costBasisRealized, List<TaxLot> lotsConsumed,
            Money realizedGainLoss) {
        public static SaleResult noOp(FifoPosition pos, Currency currency) {
            return new SaleResult(pos, Money.ZERO(currency), List.of(), Money.ZERO(currency));
        }
    }

    public sealed interface ApplyResult {

        FifoPosition newPosition();

        record Purchase(FifoPosition newPosition) implements ApplyResult {
        }

        record Sale(FifoPosition newPosition, Money costBasisRealized, List<TaxLot> lotsConsumed,
                Money realizedGainLoss) implements ApplyResult {
        }

        record NoChange(FifoPosition newPosition) implements ApplyResult {
        }
    }

}
