package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
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
    }

    /** Add a purchase (BUY) → adds a new TaxLot */
    public Position addPurchase(Quantity qty, Money costBasis, Instant acquiredDate) {
        if (qty.isNegative()) {
            throw new IllegalArgumentException("Purchase quantity must be positive");
        }
        if (costBasis.isNegative()) {
            throw new IllegalArgumentException("Cost basis cannot be negative");
        }
        if (qty.isZero() || costBasis.isZero()) {
            return this; // nothing to add
        }
        List<TaxLot> newLots = new ArrayList<>(lots);
        newLots.add(new TaxLot(qty, costBasis, acquiredDate));
        return new Position(assetSymbol, type, accountCurrency, newLots);
    }

    /**
     * Reduces position by sale quantity using FIFO (First-In-First-Out) tax lot
     * accounting.
     * Returns detailed information about the sale for tax reporting and performance
     * tracking.
     * 
     * @param sellQty      Quantity to sell (must be positive)
     * @param saleProceeds Net proceeds from sale (after fees, in account currency)
     * @return SaleResult containing new position, cost basis, consumed lots, and
     *         realized gain/loss
     * @throws IllegalStateException if trying to sell more than available
     */
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

    /** Apply a transaction to this position */
    public Position apply(Transaction tx) {
        if (tx.execution() == null || tx.execution().quantity().isZero()) {
            return this;
        }

        Quantity qty = tx.execution().quantity();

        if (qty.isPositive()) {
            // BUY: Add new tax lot
            Money lotCostBasis = tx.cashDelta().abs();
            return addPurchase(qty, lotCostBasis, tx.occurredAt());

        } else if (qty.isNegative()) {
            // SELL: Reduce position with full tracking
            Money saleProceeds = tx.cashDelta(); // Already net of fees
            SaleResult result = reduceBySale(qty.abs(), saleProceeds);

            // TODO: might want to store/log the realized gain somewhere
            // For now, we just return the new position
            return result.newPosition();
        }

        return this;
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

    /**
     * Calculates unrealized gain/loss for current holdings.
     * 
     * @param currentPrice Current market price per unit
     * @return Unrealized gain/loss (current value - cost basis)
     */
    public Money calculateUnrealizedGain(Money currentPrice) {
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
}