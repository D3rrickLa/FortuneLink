package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record Price(Money pricePerUnit) {
    public Price {
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null");
    }

    public static Price of (Money other) {
        return new Price(other);
    }

    public Money calculateValue(Quantity quantity) {
        return this.pricePerUnit.multiply(quantity.amount());
    }

    public Currency getCurrency() {
        return this.pricePerUnit.currency();
    }

    public BigDecimal getAmount() {
        return this.pricePerUnit.amount();
    }
}
