package com.laderrco.fortunelink.shared.enums;

import java.util.Currency;

public class ValidatedCurrency {
    private final Currency currency;

    // Enum-like constant
    public static final ValidatedCurrency CAD = new ValidatedCurrency("CAD");
    public static final ValidatedCurrency USD = new ValidatedCurrency("USD");
    public static final ValidatedCurrency EUR = new ValidatedCurrency("EUR");
    public static final ValidatedCurrency GBP = new ValidatedCurrency("GBP");
    public static final ValidatedCurrency JPY = new ValidatedCurrency("JPY");

    private ValidatedCurrency(String locale) {
        this.currency = java.util.Currency.getInstance(locale);
    }

    // Factory method for dynamic currencies (post-MVP)
    public static ValidatedCurrency of(String locale) {
        return new ValidatedCurrency(locale);
    }

    public String getCode() {
        return this.currency.getCurrencyCode();
    }

    public String getSymbol() {
        return this.currency.getSymbol();
    }

    public int getDefaultFractionDigits() {
        return this.currency.getDefaultFractionDigits();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ValidatedCurrency)) { return false; }
        ValidatedCurrency that = (ValidatedCurrency) o;
        return this.currency.equals(that.currency);
    }
    
    @Override
    public int hashCode() {
        return this.currency.hashCode();
    }
    
    @Override
    public String toString() {
        return this.currency.getCurrencyCode();
    }
}