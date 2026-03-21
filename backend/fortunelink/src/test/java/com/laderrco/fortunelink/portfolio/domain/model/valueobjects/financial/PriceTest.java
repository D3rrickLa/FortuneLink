package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class PriceTest {

  @Test
  void testConstructor_success() {
    Price price = new Price(Money.of(25, "USD"));
    assertNotNull(price);
  }

  @Test
  void testZeroConstructor_success() {
    Price price = Price.zero(Currency.USD);
    assertNotNull(price);
    assertEquals(Currency.USD, price.currency());
  }

  @Test
  void testConstructor_fail_priceNegative() {
    assertThatThrownBy(() -> new Price(Money.of(-25, "USD"))).isInstanceOf(
        IllegalArgumentException.class).hasMessageContaining("cannot be negative");

  }

  @Test
  void testCalculateValue_success() {
    Price price = new Price(Money.of(25, "USD"));
    Money actual = price.calculateValue(new Quantity(BigDecimal.TEN));
    assertEquals(Money.of(250, "USD"), actual);
  }

  @Test
  void testCurrency_success() {
    Price price = new Price(Money.of(25, "USD"));
    assertEquals(Currency.USD, price.currency());
  }

  @Test
  void testAmount_success() {
    Price price = new Price(Money.of(25, "USD"));
    assertEquals(Money.of(25, "USD").amount(), price.amount());
  }
}
