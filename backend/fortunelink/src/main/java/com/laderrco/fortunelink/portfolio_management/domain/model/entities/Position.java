package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.TaxLot;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// meant for 'assets' where we can AVG cost, ACB, etc.
// cash events will need their own 'positon.java'
public record Position(AssetSymbol assetSymbol, AssetType assetType, Currency accountCurrency, List<TaxLot> lots)
        implements ClassValidation {
    public Position {
        ClassValidation.validateParameter(assetSymbol, "Asset symbol cannot be null");
        ClassValidation.validateParameter(assetType, "Asset type cannot be null");
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

    public Quantity getTotalQuantity() {
        return null;
    }

    public Money getTotalCostBasis() {
        return null;
    }

    public Money calculateUnrealizedGain(Price price) {
        return null;
    }

    public Money calculateCurrentValue(Price price) {
        return null;
    }

    public Position addPurchase(Quantity quantity, Money costBasis, Instant timestamp) {
        return null;
    }

    public Position reduceBySale(Quantity quantity) {
        return null;
    }
}