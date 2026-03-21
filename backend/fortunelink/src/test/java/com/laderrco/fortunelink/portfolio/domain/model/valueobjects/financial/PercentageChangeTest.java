package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.shared.enums.Precision;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PercentageChange Value Object Unit Tests")
class PercentageChangeTest {

  @Nested
  @DisplayName("Creation and Scaling")
  class CreationTests {

    @Test
    @DisplayName("constructor_success_AppliesScaleAndMode")
    void constructor_success_AppliesScaleAndMode() {
      // Providing a raw value with too many decimals
      BigDecimal raw = new BigDecimal("0.123456789");
      PercentageChange pc = new PercentageChange(raw);

      // Should be scaled to Precision.PERCENTAGE
      assertThat(pc.change().scale()).isEqualTo(Precision.PERCENTAGE.getDecimalPlaces());
    }

    @Test
    @DisplayName("constructor_fail_NullValue")
    void constructor_fail_NullValue() {
      assertThatThrownBy(() -> new PercentageChange(null)).isInstanceOf(
          DomainArgumentException.class);
    }

    @Test
    @DisplayName("gain_success_ConvertsToDecimalForm")
    void gain_success_ConvertsToDecimalForm() {
      // 10% should become 0.10
      PercentageChange pc = PercentageChange.gain(10.0);
      assertThat(pc.change()).isEqualByComparingTo("0.10");
      assertThat(pc.isGain()).isTrue();
    }

    @Test
    @DisplayName("loss_success_ConvertsToNegativeDecimalForm")
    void loss_success_ConvertsToNegativeDecimalForm() {
      // 20% loss should become -0.20
      PercentageChange pc = PercentageChange.loss(20.0);
      assertThat(pc.change()).isEqualByComparingTo("-0.20");
      assertThat(pc.isLoss()).isTrue();
    }
  }

  @Nested
  @DisplayName("Predicates and Math")
  class PredicateTests {

    @Test
    @DisplayName("isGain_isLoss_success_ZeroBehavior")
    void isGain_isLoss_success_ZeroBehavior() {
      PercentageChange zero = new PercentageChange(BigDecimal.ZERO);
      assertThat(zero.isGain()).isFalse();
      assertThat(zero.isLoss()).isFalse();
    }

    @Test
    @DisplayName("toPercent_success_CalculatesProperly")
    void toPercent_success_CalculatesProperly() {
      PercentageChange pc = new PercentageChange(new BigDecimal("0.50"));
      // 0.50 * 100 = 50.00
      assertThat(pc.toPercent()).isEqualByComparingTo("50.00");
    }
  }

  @Nested
  @DisplayName("Comparisons")
  class ComparisonTests {

    @Test
    @DisplayName("compareTo_success_LogicChecks")
    void compareTo_success_LogicChecks() {
      PercentageChange gain = PercentageChange.gain(10);
      PercentageChange loss = PercentageChange.loss(10);
      PercentageChange deepLoss = PercentageChange.loss(50);

      assertThat(gain).isGreaterThan(loss);
      assertThat(loss).isGreaterThan(deepLoss);
      assertThat(loss).isEqualByComparingTo(PercentageChange.loss(10));
    }
  }
}