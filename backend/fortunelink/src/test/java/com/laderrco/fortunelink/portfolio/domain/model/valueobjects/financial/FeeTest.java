package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;

@DisplayName("Fee Value Object Unit Tests")
class FeeTest {
  private static final FeeType VALID_TYPE = FeeType.MANAGEMENT_FEE;
  private static final Money VALID_MONEY = Money.of(10.0, "USD");
  private static final Instant NOW = Instant.now();
  private static final Fee.FeeMetadata EMPTY_METADATA = new Fee.FeeMetadata(Map.of());

  @Nested
  @DisplayName("Constructor Validation Logic")
  class ConstructorValidation {
    @Test
    @DisplayName("constructor: success with valid parameters")
    void constructorValidParametersSuccess() {
      Fee fee = new Fee(VALID_TYPE, VALID_MONEY, null, null, NOW, EMPTY_METADATA);

      assertThat(fee.feeType()).isEqualTo(VALID_TYPE);
      assertThat(fee.nativeAmount()).isEqualTo(VALID_MONEY);
      assertThat(fee.nativeAmount().currency().getSymbol()).isEqualTo("US$");
    }

    @Test
    @DisplayName("constructor: fail on null fee type")
    void constructorNullFeeTypeThrowsException() {
      assertThatThrownBy(() -> new Fee(null, VALID_MONEY, null, null, NOW, EMPTY_METADATA))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("constructor: fail on null native amount")
    void constructorNullNativeAmountThrowsException() {
      assertThatThrownBy(() -> new Fee(VALID_TYPE, null, null, null, NOW, EMPTY_METADATA))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("constructor: fail on null occurredAt")
    void constructorNullOccurredAtThrowsException() {
      assertThatThrownBy(() -> new Fee(VALID_TYPE, VALID_MONEY, null, null, null, EMPTY_METADATA))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("constructor: fail on null metadata")
    void constructorNullMetadataThrowsException() {
      assertThatThrownBy(() -> new Fee(VALID_TYPE, VALID_MONEY, null, null, NOW, null))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("constructor: fail on negative amount")
    void constructorNegativeAmountThrowsException() {
      Money negativeMoney = Money.of(-1.0, "USD");

      assertThatThrownBy(() -> new Fee(VALID_TYPE, negativeMoney, null, null, NOW, EMPTY_METADATA))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be negative");
    }
  }

  @Nested
  @DisplayName("Static Factory Methods")
  class FactoryMethods {
    @Test
    @DisplayName("of: success with basic creation")
    void ofBasicCreationSuccess() {
      Fee fee = Fee.of(VALID_TYPE, VALID_MONEY, NOW);

      assertThat(fee.metadata().isEmpty()).isTrue();
      assertThat(fee.accountAmount()).isNull();
    }

    @Test
    @DisplayName("of: success with metadata")
    void ofWithMetadataSuccess() {
      Fee.FeeMetadata meta = new Fee.FeeMetadata(Map.of("key", "val"));

      Fee fee = Fee.of(VALID_TYPE, VALID_MONEY, NOW, meta);

      assertThat(fee.metadata().get("key")).isEqualTo("val");
    }

    @Test
    @DisplayName("withConversion: success with full state")
    void withConversionFullStateSuccess() {
      Money accountMoney = Money.of(8.5, "EUR");

      ExchangeRate rate = new ExchangeRate(
          Currency.of("USD"),
          Currency.of("EUR"),
          new BigDecimal("0.85"),
          Instant.now());

      Fee fee = Fee.withConversion(VALID_TYPE, VALID_MONEY, accountMoney, rate, NOW);

      assertThat(fee.accountAmount()).isEqualTo(accountMoney);
      assertThat(fee.exchangeRate()).isEqualTo(rate);
    }

    @Test
    @DisplayName("zero: returns standard reset state")
    void zeroReturnsStandardState() {
      Currency usd = Currency.of("USD");

      Fee zeroFee = Fee.zero(usd);

      assertThat(zeroFee.feeType()).isEqualTo(FeeType.NONE);
      assertThat(zeroFee.nativeAmount().isZero()).isTrue();
      assertThat(zeroFee.nativeAmount().currency()).isEqualTo(usd);
    }

    @Test
    @DisplayName("totalInAccountCurrency: success with multiple fees")
    void totalInAccountCurrencyMultipleFeesSuccess() {
      List<Fee> fees = List.of(
          Fee.of(VALID_TYPE, VALID_MONEY, NOW),
          Fee.of(VALID_TYPE, VALID_MONEY, NOW),
          Fee.of(VALID_TYPE, VALID_MONEY, NOW));

      Money total = Fee.totalInAccountCurrency(fees, Currency.USD);

      assertThat(total).isEqualTo(Money.of(30, "USD"));
    }

    @Test
    @DisplayName("totalInAccountCurrency: success with empty or null list")
    void totalInAccountCurrencyEmptyOrNullSuccess() {
      Money totalEmpty = Fee.totalInAccountCurrency(List.of(), Currency.USD);
      Money totalNull = Fee.totalInAccountCurrency(null, Currency.USD);

      assertThat(totalEmpty).isEqualTo(Money.of(0, "USD"));
      assertThat(totalNull).isEqualTo(Money.of(0, "USD"));
    }

    @Test
    @DisplayName("totalInAccountCurrency: fail on currency mismatch")
    void totalInAccountCurrencyCurrencyMismatchThrowsException() {
      List<Fee> fees = List.of(
          Fee.of(VALID_TYPE, VALID_MONEY, NOW),
          Fee.of(VALID_TYPE, VALID_MONEY, NOW));

      assertThatThrownBy(() -> Fee.totalInAccountCurrency(fees, Currency.JPY))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Fee currency mismatch");
    }
  }

  @Nested
  @DisplayName("FeeMetadata Operations")
  class MetadataTests {
    @Test
    @DisplayName("constructor: handles null map")
    void metadataConstructorHandlesNullMap() {
      Fee.FeeMetadata meta = new Fee.FeeMetadata(null);

      assertThat(meta.values()).isNotNull();
      assertThat(meta.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("with: returns new instance with added value")
    void withAddsValueImmutably() {
      Fee.FeeMetadata initial = new Fee.FeeMetadata(Map.of("A", "1"));

      Fee.FeeMetadata updated = initial.with("B", "2");

      assertThat(initial.containsKey("B")).isFalse();
      assertThat(updated.get("B")).isEqualTo("2");
      assertThat(updated.get("A")).isEqualTo("1");
    }

    @Test
    @DisplayName("withAll: merges maps correctly")
    void withAllMergesMapsCorrectly() {
      Fee.FeeMetadata initial = new Fee.FeeMetadata(Map.of("A", "1"));
      Map<String, String> extra = Map.of("B", "2", "C", "3");

      Fee.FeeMetadata merged = initial.withAll(extra);

      assertThat(merged.values()).hasSize(3).containsKeys("A", "B", "C");
    }

    @Test
    @DisplayName("getOrDefault: returns fallback when key missing")
    void getOrDefaultReturnsFallbackWhenMissing() {
      Fee.FeeMetadata meta = new Fee.FeeMetadata(Map.of("exists", "yes"));

      assertThat(meta.getOrDefault("exists", "no")).isEqualTo("yes");
      assertThat(meta.getOrDefault("missing", "fallback")).isEqualTo("fallback");
    }
  }
}