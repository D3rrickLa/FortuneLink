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
public record Position(AssetSymbol assetSymbol, AssetType type, List<TaxLot> lots) implements ClassValidation {
    public Position {
        ClassValidation.validateParameter(assetSymbol, "Asset symbol cannot be null");
        ClassValidation.validateParameter(type, "Asset type cannot be null");
        lots = lots == null ? List.of() : List.copyOf(lots);
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
                .reduce(Money.ZERO(lots.isEmpty() ? Currency.USD : lots.get(0).costBasis().currency()), Money::add);
    }

    /** Add a purchase (BUY) → adds a new TaxLot */
    public Position addPurchase(Quantity qty, Money costBasis, Instant acquiredDate) {
        if (qty.isZero() || costBasis.isZero()) {
            return this; // nothing to add
        }
        List<TaxLot> newLots = new ArrayList<>(lots);
        newLots.add(new TaxLot(qty, costBasis, acquiredDate));
        return new Position(assetSymbol, type, newLots);
    }

    /** Reduce by a sale (SELL) → consume lots FIFO */
    public Position reduceBySale(Quantity sellQty) {
        if (sellQty.isZero())
            return this;

        List<TaxLot> remaining = new ArrayList<>();
        Quantity remainingToSell = sellQty;

        for (TaxLot lot : lots) {
            if (remainingToSell.isZero()) {
                remaining.add(lot);
            } else if (lot.quantity().compareTo(remainingToSell) <= 0) {
                // consume entire lot
                remainingToSell = remainingToSell.subtract(lot.quantity());
            } else {
                // partially consume lot
                remaining.add(lot.reduce(remainingToSell));
                remainingToSell = Quantity.ZERO;
            }
        }

        if (!remainingToSell.isZero()) {
            throw new IllegalStateException("Selling more than available in position");
        }

        return new Position(assetSymbol, type, remaining);
    }

    /** Apply a transaction to this position */
    public Position apply(Transaction tx) {
        // ignore non-trades
        if (tx.execution() == null || tx.execution().quantity().isZero()) {
            return this;
        }

        Quantity qty = tx.execution().quantity();
        Money costBasis = tx.costBasisDelta(); // assumes already includes fees
        Instant date = tx.occurredAt();

        if (qty.isPositive()) {
            return addPurchase(qty, costBasis, date);
        } else if (qty.isNegative()) {
            return reduceBySale(qty.abs());
        } else {
            return this;
        }
    }

    /** Unmodifiable view of lots */
    public List<TaxLot> getLots() {
        return Collections.unmodifiableList(lots);
    }
}