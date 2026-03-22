package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;

@DisplayName("ExchangeRate Value Object Unit Tests")
class ExchangeRateTest {
  private final Currency USD = Currency.of("USD");
  private final Currency EUR = Currency.of("EUR");
  private final Instant NOW = Instant.now();
  private final BigDecimal VALID_RATE = new BigDecimal("0.8500");

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructorTests {
    @Test
    @DisplayName("constructor: success with valid initialization")
    void constructorValidInitializationSuccess() {
      ExchangeRate rate = new ExchangeRate(USD, EUR, VALID_RATE, NOW);

      assertThat(rate.from()).isEqualTo(USD);
      assertThat(rate.to()).isEqualTo(EUR);
      assertThat(rate.rate()).isEqualByComparingTo(VALID_RATE);
    }

    @Test
    @DisplayName("constructor: fail on null parameters")
    void constructorNullParametersThrowsException() {
      assertThatThrownBy(() -> new ExchangeRate(null, EUR, VALID_RATE, NOW))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("constructor: fail on non-positive rate")
    void constructorNonPositiveRateThrowsException() {
      assertThatThrownBy(() -> new ExchangeRate(USD, EUR, BigDecimal.ZERO, NOW))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must be positive");

      assertThatThrownBy(() -> new ExchangeRate(USD, EUR, new BigDecimal("-1.5"), NOW))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Conversion Logic")
  class ConversionTests {
    @Test
    @DisplayName("convert: success with correct calculation")
    void convertValidMoneySuccess() {
      ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.90"), NOW);
      Money input = Money.of(100, "USD");

      Money result = rate.convert(input);

      assertThat(result.currency()).isEqualTo(EUR);
      assertThat(result.amount()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("convert: fail on currency mismatch")
    void convertCurrencyMismatchThrowsException() {
      ExchangeRate rate = new ExchangeRate(USD, EUR, VALID_RATE, NOW);
      Money invalid = Money.of(100, "GBP");

      assertThatThrownBy(() -> rate.convert(invalid))
          .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("convert: fail on null money")
    void convertNullMoneyThrowsException() {
      ExchangeRate rate = new ExchangeRate(USD, EUR, VALID_RATE, NOW);

      assertThatThrownBy(() -> rate.convert(null))
          .isInstanceOf(DomainArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Identity and Inverse")
  class IdentityAndInverseTests {
    @Test
    @DisplayName("identity: returns rate of one")
    void identityReturnsRateOfOne() {
      ExchangeRate identity = ExchangeRate.identity(USD, NOW);

      assertThat(identity.from()).isEqualTo(USD);
      assertThat(identity.to()).isEqualTo(USD);
      assertThat(identity.rate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("inverse: flips currencies and rate")
    void inverseFlipsCurrenciesAndRate() {
      ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("2.0"), NOW);

      ExchangeRate inverse = rate.inverse();

      assertThat(inverse.from()).isEqualTo(EUR);
      assertThat(inverse.to()).isEqualTo(USD);
      assertThat(inverse.rate()).isEqualByComparingTo("0.5");
    }
  }
}