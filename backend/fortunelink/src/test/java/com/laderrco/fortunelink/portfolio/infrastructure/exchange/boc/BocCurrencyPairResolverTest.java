package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BocCurrencyPairResolver Tests")
class BocCurrencyPairResolverTest {

  @Nested
  @DisplayName("Direct CAD Pair Resolution")
  class DirectPairTests {

    @Test
    @DisplayName("should resolve correctly when target is CAD")
    void shouldResolveWhenTargetIsCad() {
      List<String> result = BocCurrencyPairResolver.resolveSeries("USD", "CAD");

      assertThat(result)
          .hasSize(1)
          .containsExactly("FXUSDCAD");
    }

    @Test
    @DisplayName("should resolve correctly when base is CAD")
    void shouldResolveWhenBaseIsCad() {
      List<String> result = BocCurrencyPairResolver.resolveSeries("CAD", "USD");

      assertThat(result)
          .hasSize(1)
          .containsExactly("FXCADUSD");
    }

    @Test
    @DisplayName("should handle lowercase inputs correctly")
    void shouldHandleLowercaseInputs() {
      List<String> result = BocCurrencyPairResolver.resolveSeries("eur", "cad");

      assertThat(result).containsExactly("FXEURCAD");
    }
  }

  @Nested
  @DisplayName("Cross-Currency Resolution")
  class CrossCurrencyTests {

    @Test
    @DisplayName("should resolve two series for cross-currency pairs via CAD")
    void shouldResolveViaCadForNonCadPairs() {
      // EUR to USD needs: EUR -> CAD and CAD -> USD
      List<String> result = BocCurrencyPairResolver.resolveSeries("EUR", "USD");

      assertThat(result)
          .hasSize(2)
          .containsExactly("FXEURCAD", "FXCADUSD");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Validation")
  class ValidationTests {

    @Test
    @DisplayName("should throw IllegalArgumentException when currencies are the same")
    void shouldThrowExceptionForSameCurrency() {
      assertThatThrownBy(() -> BocCurrencyPairResolver.resolveSeries("USD", "USD"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be the same");
    }
  }
}