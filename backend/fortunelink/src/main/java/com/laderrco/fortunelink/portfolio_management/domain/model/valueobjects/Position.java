package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// a derived VO through processing all transaction for a given symbol
// if you sold/held this asset today, what is my quantity, cost basis, and unrealized gain?
public record Position(AssetSymbol assetSymbol, AssetType type, List<TaxLot> lots) implements ClassValidation {
    public Position {
        ClassValidation.validateParameter(assetSymbol, "Asset symbol cannot be null");
        ClassValidation.validateParameter(type, "Asset type cannot be null");
        lots = lots == null ? List.of() : List.copyOf(lots);
    }

    /** Total quantity across all lots */
    public Quantity getTotalQuantity() {
        return lots.stream()
                .map(TaxLot::quantity)
                .reduce(Quantity.ZERO, Quantity::add);
    }

    /** Total cost basis across all lots */
    public Money getTotalCostBasis() {
        return lots.stream()
                .map(TaxLot::costBasis)
                .reduce(Money.ZERO(lots.isEmpty() ? Currency.USD : lots.get(0).costBasis().currency()),
                        Money::add);
    }

    /** Adds a new purchase as a new TaxLot */
    public Position addPurchase(Quantity quantity, Money costBasis, Instant acquiredDate) {
        ClassValidation.validateParameter(quantity);
        ClassValidation.validateParameter(costBasis);
        ClassValidation.validateParameter(acquiredDate);

        List<TaxLot> newLots = new ArrayList<>(lots);
        newLots.add(new TaxLot(quantity, costBasis, acquiredDate));
        return new Position(assetSymbol, type, newLots);
    }

    /** Reduces position by selling quantity (FIFO) */
    public Position reduceBySale(Quantity sellQuantity) {
        if (sellQuantity.isNegative() || sellQuantity.isZero()) {
            throw new IllegalArgumentException("Sell quantity must be positive");
        }

        List<TaxLot> newLots = new ArrayList<>();
        Quantity remainingToSell = sellQuantity;

        for (TaxLot lot : lots) {
            if (remainingToSell.isZero()) {
                newLots.add(lot);
            } else if (lot.quantity().compareTo(remainingToSell) <= 0) {
                // Entire lot consumed
                remainingToSell = remainingToSell.subtract(lot.quantity());
            } else {
                // Partially consume this lot
                Quantity remainingLotQty = lot.quantity().subtract(remainingToSell);
                Money remainingCostBasis = lot.proportionalCost(remainingLotQty);
                newLots.add(new TaxLot(remainingLotQty, remainingCostBasis, lot.acquiredDate()));
                remainingToSell = Quantity.ZERO;
            }
        }

        if (!remainingToSell.isZero()) {
            throw new IllegalArgumentException("Not enough quantity to sell from position");
        }

        return new Position(assetSymbol, type, newLots);
    }

    /** Calculate current value given market price */
    public Money calculateCurrentValue(Money marketPrice) {
        return marketPrice.multiply(getTotalQuantity().amount());
    }

    /** Calculate unrealized gain */
    public Money calculateUnrealizedGain(Money marketPrice) {
        Money currentValue = calculateCurrentValue(marketPrice);
        return currentValue.subtract(getTotalCostBasis());
    }

    /** Tax lots are exposed as immutable list */
    public List<TaxLot> getLots() {
        return Collections.unmodifiableList(lots);
    }
}