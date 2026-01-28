package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record CashIdentifier(String id, ValidatedCurrency currency) implements AssetIdentifier, ClassValidation {
    public CashIdentifier(String id, ValidatedCurrency currency) {
        Objects.requireNonNull(id);
        this.id = id;
        this.currency = ValidatedCurrency.of(id);
        
    }

    public CashIdentifier(String id) {
        this(id, ValidatedCurrency.of(id));
    }

    @Override
    public String getPrimaryId() {
        return this.id;
    }

    @Override
    public String displayName() {
        return String.format("CASH: %s", ValidatedCurrency.of(getPrimaryId()));
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.CASH;
    }
    
}
