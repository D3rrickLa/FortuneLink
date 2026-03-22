package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.shared.enums.Precision;

@DisplayName("PercentageChange Value Object Unit Tests")
class PercentageChangeTest {
  @Nested
  @DisplayName("Creation and Scaling")
  class CreationTests {
    @Test
    @DisplayName("constructor: success applies scale and precision")
    void constructorAppliesCorrectScale() {
      BigDecimal raw = new BigDecimal("0.123456789");
      PercentageChange pc = new PercentageChange(raw);

      assertThat(pc.change().scale()).isEqualTo(Precision.PERCENTAGE.getDecimalPlaces());
    }

    @Test
    @DisplayName("constructor: fail on null value")
    void constructorThrowsOnNullValue() {
      assertThatThrownBy(() -> new PercentageChange(null))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("factories: gain and loss convert percentages correctly")
    void factoryMethodsCreateCorrectDecimalValues() {
      PercentageChange gain = PercentageChange.gain(10.0);
      PercentageChange loss = PercentageChange.loss(20.0);

      assertThat(gain.change()).isEqualByComparingTo("0.10");
      assertThat(gain.isGain()).isTrue();
      assertThat(loss.change()).isEqualByComparingTo("-0.20");
      assertThat(loss.isLoss()).isTrue();
    }
  }

  @Nested
  @DisplayName("Predicates and Math")
  class PredicateTests {
    @Test
    @DisplayName("predicates: handles zero behavior and percent conversion")
    void predicatesAndMathPerformCorrectLogic() {
      PercentageChange zero = new PercentageChange(BigDecimal.ZERO);
      PercentageChange half = new PercentageChange(new BigDecimal("0.50"));

      assertThat(zero.isGain()).isFalse();
      assertThat(zero.isLoss()).isFalse();
      assertThat(half.toPercent()).isEqualByComparingTo("50.00");
    }
  }

  @Nested
  @DisplayName("Comparisons")
  class ComparisonTests {
    @Test
    @DisplayName("compareTo: orders gains and losses correctly")
    void percentageChangesOrderCorrectly() {
      PercentageChange gain = PercentageChange.gain(10);
      PercentageChange loss = PercentageChange.loss(10);
      PercentageChange deepLoss = PercentageChange.loss(50);

      assertThat(gain).isGreaterThan(loss);
      assertThat(loss).isGreaterThan(deepLoss);
      assertThat(loss).isEqualByComparingTo(PercentageChange.loss(10));
    }
  }
}