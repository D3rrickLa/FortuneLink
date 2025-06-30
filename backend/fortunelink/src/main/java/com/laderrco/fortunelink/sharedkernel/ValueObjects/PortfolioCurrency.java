package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.CryptoSymbols;

/*
 * We are using the built in Java Currency class for the backbone of this custom Currency Value Object
 */
public record PortfolioCurrency(java.util.Currency javaCurrency) {
    public PortfolioCurrency {
        Objects.requireNonNull(javaCurrency, "java.util.Currency cannot be null.");
    }

    public String code() {
        return javaCurrency.getCurrencyCode();
    }

    public int getDefaultScale() {
        return javaCurrency.getDefaultFractionDigits();
    }

    public String getSymbol() {
        return javaCurrency.getSymbol();
    }

    public boolean isFiat(String code) {
        return !CryptoSymbols.isCrypto(code);
    }

}
