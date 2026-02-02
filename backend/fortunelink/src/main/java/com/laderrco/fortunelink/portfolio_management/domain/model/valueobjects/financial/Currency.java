package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@SuppressWarnings("unused")
public class Currency {
    private final java.util.Currency currency;

    private static final Currency CAD  = new Currency("CAD");
    private static final Currency EUR  = new Currency("EUR");
    private static final Currency GBP  = new Currency("GBP");
    private static final Currency JPY  = new Currency("JPY");
    private static final Currency USD  = new Currency("USD");

    private Currency(String locale) {
        this.currency = java.util.Currency.getInstance(locale);
    }

    public static Currency of(String locale) {
        return new Currency(locale);
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
}