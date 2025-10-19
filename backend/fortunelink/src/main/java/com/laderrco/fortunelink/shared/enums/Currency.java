package com.laderrco.fortunelink.shared.enums;

public class Currency {
    private final java.util.Currency javaCurrency;

    // Enum-like constant
    public static final Currency CAD = new Currency("CAD");
    public static final Currency USD = new Currency("USD");
    public static final Currency EUR = new Currency("EUR");
    public static final Currency GBP = new Currency("GBP");
    public static final Currency JPY = new Currency("JPY");

    private Currency(String locale) {
        this.javaCurrency = java.util.Currency.getInstance(locale);
    }

    // Factory method for dynamic currencies (post-MVP)
    public static Currency of(String locale) {
        return new Currency(locale);
    }

    public String getCode() {
        return this.javaCurrency.getCurrencyCode();
    }

    public String getSymbol() {
        return this.javaCurrency.getSymbol();
    }

    public int getDefaultFractionDigits() {
        return this.javaCurrency.getDefaultFractionDigits();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Currency)) { return false; }
        Currency that = (Currency) o;
        return this.javaCurrency.equals(that.javaCurrency);
    }
    
    @Override
    public int hashCode() {
        return this.javaCurrency.hashCode();
    }
    
    @Override
    public String toString() {
        return this.javaCurrency.getCurrencyCode();
    }

}