package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.util.Objects;

import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public record AllocationItem(AssetIdentifier assetIdentifier, Money value, Percentage percentage) {
    public AllocationItem {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(value, "Value cannot be null.");
        Objects.requireNonNull(percentage, "Percentage cannot be null.");
    }
}