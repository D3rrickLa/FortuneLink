package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// a derived VO through processing all transaction for a given symbol
// if you sold/held this asset today, what is my quantity, cost basis, and unrealized gain?
// it is made up of tax lots to kep keep track of the above
public record Position(AssetSymbol assetSymbol, AssetType type, Currency accountCurrency, List<TaxLot> lots)
        implements ClassValidation {
    public Position {
        ClassValidation.validateParameter(assetSymbol, "Asset symbol cannot be null");
        ClassValidation.validateParameter(type, "Asset type cannot be null");
        lots = lots == null ? List.of() : List.copyOf(lots);

        // Validate all lots match Acc. currency
        for (TaxLot lot : lots) {
            if (!lot.costBasis().currency().equals(accountCurrency)) {
                throw new IllegalArgumentException(
                        String.format("TaxLot currency (%s) doesn't match account currency (%s)",
                                lot.costBasis().currency(), accountCurrency));
            }
        }
    }

    // empty position
    public static Position empty(AssetSymbol symbol, AssetType type, Currency currency) {
        return new Position(symbol, type, currency, List.of());
    }

    // Add a purchase - buy - adding to a new TaxLot
    public Position addPurchase(Quantity qty, Money costBasis, Instant acquiredDate) {
        if (qty.isNegative()) {
            throw new IllegalArgumentException("Purchase quantity must be positive, got: " + qty);
        }

        if (costBasis.isNegative()) {
            throw new IllegalArgumentException("Cost basis cannot be negative, got: " + costBasis);
        }
        
        if (!costBasis.currency().equals(accountCurrency)) {
            throw new IllegalArgumentException(
                    String.format("Cost basis currency (%s) must match account currency (%s)",
                            costBasis.currency(), accountCurrency));
        }

        if (qty.isZero() || costBasis.isZero()) {
            return this; // nothing to add
        }

        List<TaxLot> newLots = new ArrayList<>(lots);
        newLots.add(new TaxLot(qty, costBasis, acquiredDate));
        return new Position(assetSymbol, type, accountCurrency, newLots);
    }

    // reduces position by sale qty via FIFO
    public SaleResult reduceBySale(Quantity sellQty, Money saleProceeds) {
        if (sellQty.isZero()) {
            return new SaleResult(
                    this,
                    Money.ZERO(accountCurrency),
                    List.of(),
                    Money.ZERO(accountCurrency));
        }

        if (sellQty.isNegative()) {
            throw new IllegalArgumentException("Sell quantity must be positive, got: " + sellQty);
        }

        if (!saleProceeds.currency().equals(accountCurrency)) {
            throw new IllegalArgumentException(
                    String.format("Sale proceeds currency (%s) must match account currency (%s)",
                            saleProceeds.currency(), accountCurrency));
        }

        List<TaxLot> remaining = new ArrayList<>();
        List<TaxLot> consumed = new ArrayList<>();
        Quantity remainingToSell = sellQty;
        Money totalCostRealized = Money.ZERO(accountCurrency);

        for (TaxLot lot : lots) {
            if (remainingToSell.isZero()) {
                // No more to sell, keep remaining lots
                remaining.add(lot);
            } else if (lot.quantity().compareTo(remainingToSell) <= 0) {
                // Consume entire lot
                consumed.add(lot);
                totalCostRealized = totalCostRealized.add(lot.costBasis());
                remainingToSell = remainingToSell.subtract(lot.quantity());
            } else {
                // Partially consume lot
                Money partialCost = lot.proportionalCost(remainingToSell);
                TaxLot soldPortion = new TaxLot(
                        remainingToSell,
                        partialCost,
                        lot.acquiredDate());
                consumed.add(soldPortion);
                totalCostRealized = totalCostRealized.add(partialCost);
                remaining.add(lot.reduce(remainingToSell));
                remainingToSell = Quantity.ZERO;
            }
        }

        if (!remainingToSell.isZero()) {
            throw new IllegalStateException(
                    String.format("Attempted to sell %s but only %s available in position %s",
                            sellQty, getTotalQuantity(), assetSymbol));
        }

        // Calculate realized gain/loss
        Money realizedGainLoss = saleProceeds.subtract(totalCostRealized);

        return new SaleResult(
                new Position(assetSymbol, type, accountCurrency, remaining),
                totalCostRealized,
                consumed,
                realizedGainLoss);
    }

    public ApplyResult apply(Transaction tx) {
        if (tx.execution() == null || tx.execution().quantity().isZero()) {
            return new ApplyResult.NoChange(this);
        }

        Quantity qty = tx.execution().quantity();

        if (qty.isPositive()) {
            // BUY: Add new tax lot
            Money lotCostBasis = tx.cashDelta().abs();
            Position updated = addPurchase(qty, lotCostBasis, tx.occurredAt());
            return new ApplyResult.Purchase(updated);

        } else if (qty.isNegative()) {
            // SELL: Reduce position with full tracking
            Money saleProceeds = tx.cashDelta(); // Already net of fees
            SaleResult result = reduceBySale(qty.abs(), saleProceeds);

            return new ApplyResult.Sale(
                    result.newPosition(),
                    result.costBasisRealized(),
                    result.lotsConsumed(),
                    result.realizedGainLoss());
        }

        return new ApplyResult.NoChange(this);
    }

    /** Unmodifiable view of lots */
    public List<TaxLot> getLots() {
        return Collections.unmodifiableList(lots);
    }

    /** Total quantity held */
    public Quantity getTotalQuantity() {
        return lots.stream()
                .map(TaxLot::quantity)
                .reduce(Quantity.ZERO, Quantity::add);
    }

    /** Total cost basis (account currency) */
    public Money getTotalCostBasis() {
        return lots.stream()
                .map(TaxLot::costBasis)
                .reduce(Money.ZERO(accountCurrency), Money::add);
    }

    /** Check if position is empty (no shares held) */
    public boolean isEmpty() {
        return getTotalQuantity().isZero();
    }

    /**
     * Calculates unrealized gain/loss for current holdings.
     * 
     * @param currentPrice Current market price per unit
     * @return Unrealized gain/loss (current value - cost basis)
     */
    public Money calculateUnrealizedGain(Money currentPrice) {
        if (!currentPrice.currency().equals(accountCurrency)) {
            throw new IllegalArgumentException(
                    String.format("Price currency (%s) must match account currency (%s)",
                            currentPrice.currency(), accountCurrency));
        }
        Money currentValue = currentPrice.multiply(getTotalQuantity().amount());
        Money costBasis = getTotalCostBasis();
        return currentValue.subtract(costBasis);
    }

    /**
     * Calculates current market value of position.
     * 
     * @param currentPrice Current market price per unit
     * @return Total market value
     */
    public Money calculateCurrentValue(Money currentPrice) {
        if (!currentPrice.currency().equals(accountCurrency)) {
            throw new IllegalArgumentException(
                    String.format("Price currency (%s) must match account currency (%s)",
                            currentPrice.currency(), accountCurrency));
        }
        return currentPrice.multiply(getTotalQuantity().amount());
    }

    public record SaleResult(
            Position newPosition,
            Money costBasisRealized,
            List<TaxLot> lotsConsumed,
            Money realizedGainLoss) {
        public SaleResult {
            lotsConsumed = List.copyOf(lotsConsumed);
        }
    }

    public sealed interface ApplyResult {
        Position newPosition();

        /**
         * Transaction resulted in a purchase (BUY)
         */
        record Purchase(Position newPosition) implements ApplyResult {
        }

        /**
         * Transaction resulted in a sale (SELL)
         */
        record Sale(
                Position newPosition,
                Money costBasisRealized,
                List<TaxLot> lotsConsumed,
                Money realizedGainLoss) implements ApplyResult {
            public Sale {
                lotsConsumed = List.copyOf(lotsConsumed);
            }
        }

        /**
         * Transaction didn't change the position (no trade execution)
         */
        record NoChange(Position newPosition) implements ApplyResult {
        }
    }

}