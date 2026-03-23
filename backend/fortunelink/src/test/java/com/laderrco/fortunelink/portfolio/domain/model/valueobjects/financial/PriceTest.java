package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Price Value Object Unit Tests")
public class PriceTest {
  @Test
  @DisplayName("constructor: success on valid initialization")
  void constructorInitializesCorrectly() {
    Price price = new Price(Money.of(25, "USD"));
    assertThat(price).isNotNull();
  }

  @Test
  @DisplayName("zero: creates zero price with correct currency")
  void zeroCreatesPriceWithCorrectCurrency() {
    Price price = Price.zero(Currency.USD);

    assertThat(price).isNotNull();
    assertThat(price.currency()).isEqualTo(Currency.USD);
  }

  @Test
  @DisplayName("constructor: fail when price is negative")
  void constructorThrowsOnNegativePrice() {
    assertThatThrownBy(() -> new Price(Money.of(-25, "USD"))).isInstanceOf(
        IllegalArgumentException.class).hasMessageContaining("cannot be negative");
  }

  @Test
  @DisplayName("calculateValue: success on simple multiplication")
  void calculateValueReturnsProductOfPriceAndQuantity() {
    Price price = new Price(Money.of(25, "USD"));
    Money actual = price.calculateValue(new Quantity(BigDecimal.TEN));

    assertThat(actual).isEqualTo(Money.of(250, "USD"));
  }

  @Test
  @DisplayName("currency: returns correct currency")
  void currencyReturnsExpectedValue() {
    Price price = new Price(Money.of(25, "USD"));
    assertThat(price.currency()).isEqualTo(Currency.USD);
  }

  @Test
  @DisplayName("amount: returns correct money amount")
  void amountReturnsExpectedMoneyValue() {
    Price price = new Price(Money.of(25, "USD"));
    assertThat(price.amount()).isEqualTo(Money.of(25, "USD").amount());
  }
}