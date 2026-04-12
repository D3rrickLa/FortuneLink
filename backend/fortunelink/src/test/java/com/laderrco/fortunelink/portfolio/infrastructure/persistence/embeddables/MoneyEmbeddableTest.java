package com.laderrco.fortunelink.portfolio.infrastructure.persistence.embeddables;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoneyEmbeddable Unit Tests")
class MoneyEmbeddableTest {

  @Test
  @DisplayName("Constructor and Getters should preserve values and precision")
  void constructorAndGettersShouldWork() {
    // Arrange
    BigDecimal amount = new BigDecimal("1234.5678901234");
    String currency = "USD";

    // Act
    MoneyEmbeddable embeddable = new MoneyEmbeddable(amount, currency);

    // Assert
    assertThat(embeddable.getAmount()).isEqualByComparingTo(amount);
    assertThat(embeddable.getCurrencyCode()).isEqualTo("USD");
  }

  @Test
  @DisplayName("JPA requirements: should have a protected no-args constructor")
  void shouldHaveProtectedNoArgsConstructor() {
    // This test ensures that Hibernate/JPA can instantiate the class via reflection
    MoneyEmbeddable embeddable = new MoneyEmbeddable() {
    };

    assertThat(embeddable).isNotNull();
    assertThat(embeddable.getAmount()).isNull();
    assertThat(embeddable.getCurrencyCode()).isNull();
  }

  @Test
  @DisplayName("Should handle zero and negative amounts for edge case mapping")
  void shouldHandleZeroAndNegative() {
    MoneyEmbeddable zero = new MoneyEmbeddable(BigDecimal.ZERO, "EUR");
    MoneyEmbeddable negative = new MoneyEmbeddable(new BigDecimal("-100.00"), "CAD");

    assertThat(zero.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(negative.getAmount()).isEqualByComparingTo("-100");
  }
}