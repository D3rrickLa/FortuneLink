package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import java.util.Locale;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Currency {
  public static final Currency CAD = new Currency("CAD");
  public static final Currency EUR = new Currency("EUR");
  public static final Currency GBP = new Currency("GBP");
  public static final Currency JPY = new Currency("JPY");
  public static final Currency USD = new Currency("USD");
  private final java.util.Currency currency;

  private Currency(String locale) {
    this.currency = java.util.Currency.getInstance(locale);
  }

  public static Currency of(String currencyCode) {
    try {
      return new Currency(currencyCode.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "Unsupported currency code: " + currencyCode,
          ex);
    }
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