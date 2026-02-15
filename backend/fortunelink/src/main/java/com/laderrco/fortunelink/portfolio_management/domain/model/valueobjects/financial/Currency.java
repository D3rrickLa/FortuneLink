package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.util.Locale;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class Currency {
    private final java.util.Currency currency;

    public static final Currency CAD  = new Currency("CAD");
    public static final Currency EUR  = new Currency("EUR");
    public static final Currency GBP  = new Currency("GBP");
    public static final Currency JPY  = new Currency("JPY");
    public static final Currency USD  = new Currency("USD");

    private Currency(String locale) {
        this.currency = java.util.Currency.getInstance(locale);
    }

    public static Currency of(String locale) {
        return new Currency(locale);
    }

    public static Currency fromLocale(Locale locale) {
        java.util.Currency currency = java.util.Currency.getInstance(locale);
        return new Currency(currency);
    }

    // i.e. USD
    public String getCode() {
        return this.currency.getCurrencyCode();
    }

    // i.e. $US
    public String getSymbol() {
        return this.currency.getSymbol();
    }

    public int getDefaultFractionDigits() {
        return this.currency.getDefaultFractionDigits();
    }
}