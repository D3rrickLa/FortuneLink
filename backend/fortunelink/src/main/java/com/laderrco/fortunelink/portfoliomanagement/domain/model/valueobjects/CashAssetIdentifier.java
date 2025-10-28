package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.shared.enums.Currency;

public record CashAssetIdentifier(UUID id, Currency currency) implements AssetIdentifier {
    public CashAssetIdentifier {
        Objects.requireNonNull(id);
        Objects.requireNonNull(currency);

    }
    @Override
    public String displayName() {
        return currency.toString();
    }

    
}