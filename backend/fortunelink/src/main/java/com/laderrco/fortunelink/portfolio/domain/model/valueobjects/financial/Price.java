package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import java.math.BigDecimal;

public record Price(Money pricePerUnit) {
  public Price {
    notNull(pricePerUnit, "pricePerUnit");
    if (pricePerUnit.isNegative()) {
      throw new IllegalArgumentException("Price cannot be negative");
    }
  }

  public static Price ZERO(Currency currency) {
    return new Price(Money.zero(currency));
  }

  public static Price of(BigDecimal amount, Currency currency) {
    return new Price(new Money(amount, currency));
  }

  public static Price of(String amount, Currency currency) {
    return new Price(new Money(new BigDecimal(amount), currency));
  }

  public Money calculateValue(Quantity quantity) {
    notNull(quantity, "quantity");
    return pricePerUnit.multiply(quantity.amount());
  }

  public Currency currency() {
    return pricePerUnit.currency();
  }

  public BigDecimal amount() {
    return pricePerUnit.amount();
  }
}