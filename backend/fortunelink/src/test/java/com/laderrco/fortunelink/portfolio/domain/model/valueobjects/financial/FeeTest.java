package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import org.junit.jupiter.api.*;

import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;

@DisplayName("Fee Value Object Unit Tests")
class FeeTest {

    private final FeeType VALID_TYPE = FeeType.MANAGEMENT_FEE;
    private final Money VALID_MONEY = Money.of(10.0, "USD");
    private final Instant NOW = Instant.now();
    private final Fee.FeeMetadata EMPTY_METADATA = new Fee.FeeMetadata(Map.of());

    @Nested
    @DisplayName("Constructor Validation Logic")
    class ConstructorValidation {

        @Test
        @DisplayName("constructor_success_ValidParameters")
        void constructor_success_ValidParameters() {
            Fee fee = new Fee(VALID_TYPE, VALID_MONEY, null, null, NOW, EMPTY_METADATA);
            assertThat(fee.feeType()).isEqualTo(VALID_TYPE);
            assertThat(fee.nativeAmount()).isEqualTo(VALID_MONEY);
            assertThat(fee.nativeAmount().currency().getSymbol()).isEqualTo("US$");
        }

        @Test
        @DisplayName("constructor_fail_NullFeeType")
        void constructor_fail_NullFeeType() {
            assertThatThrownBy(() -> new Fee(null, VALID_MONEY, null, null, NOW, EMPTY_METADATA))
                    .isInstanceOf(DomainArgumentException.class);
        }

        @Test
        @DisplayName("constructor_fail_NullNativeAmount")
        void constructor_fail_NullNativeAmount() {
            assertThatThrownBy(() -> new Fee(VALID_TYPE, null, null, null, NOW, EMPTY_METADATA))
                    .isInstanceOf(DomainArgumentException.class);
        }

        @Test
        @DisplayName("constructor_fail_NullOccurredAt")
        void constructor_fail_NullOccurredAt() {
            assertThatThrownBy(() -> new Fee(VALID_TYPE, VALID_MONEY, null, null, null, EMPTY_METADATA))
                    .isInstanceOf(DomainArgumentException.class);
        }

        @Test
        @DisplayName("constructor_fail_NullMetadata")
        void constructor_fail_NullMetadata() {
            assertThatThrownBy(() -> new Fee(VALID_TYPE, VALID_MONEY, null, null, NOW, null))
                    .isInstanceOf(DomainArgumentException.class);
        }

        @Test
        @DisplayName("constructor_fail_NegativeAmount")
        void constructor_fail_NegativeAmount() {
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
        @DisplayName("of_success_BasicCreation")
        void of_success_BasicCreation() {
            Fee fee = Fee.of(VALID_TYPE, VALID_MONEY, NOW);
            assertThat(fee.metadata().isEmpty()).isTrue();
            assertThat(fee.accountAmount()).isNull();
        }

        @Test
        @DisplayName("of_success_WithMetadata")
        void of_success_WithMetadata() {
            Fee.FeeMetadata meta = new Fee.FeeMetadata(Map.of("key", "val"));
            Fee fee = Fee.of(VALID_TYPE, VALID_MONEY, NOW, meta);
            assertThat(fee.metadata().get("key")).isEqualTo("val");
        }

        @Test
        @DisplayName("withConversion_success_FullState")
        void withConversion_success_FullState() {
            Money accountMoney = Money.of(8.5, "EUR");
            // Assuming ExchangeRate dummy
            ExchangeRate rate = new ExchangeRate(Currency.of("USD"), Currency.of("EUR"), new BigDecimal("0.85"), Instant.now());

            Fee fee = Fee.withConversion(VALID_TYPE, VALID_MONEY, accountMoney, rate, NOW);

            assertThat(fee.accountAmount()).isEqualTo(accountMoney);
            assertThat(fee.exchangeRate()).isEqualTo(rate);
        }

        @Test
        @DisplayName("ZERO_success_StandardResetState")
        void ZERO_success_StandardResetState() {
            Currency usd = Currency.of("USD");
            Fee zeroFee = Fee.ZERO(usd);

            assertThat(zeroFee.feeType()).isEqualTo(FeeType.NONE);
            assertThat(zeroFee.nativeAmount().isZero()).isTrue();
            assertThat(zeroFee.nativeAmount().currency()).isEqualTo(usd);
        }

        @Test
        void testTotalInAccountCurrency_Success_Default() {
            Fee fee_1 = Fee.of(VALID_TYPE, VALID_MONEY, NOW);
            Fee fee_2 = Fee.of(VALID_TYPE, VALID_MONEY, NOW);
            Fee fee_3 = Fee.of(VALID_TYPE, VALID_MONEY, NOW);

            List<Fee> fees = List.of(fee_1, fee_2, fee_3);
            Money actualTotal = Fee.totalInAccountCurrency(fees, Currency.USD);
            assertEquals(Money.of(30, "USD"), actualTotal);
        }

        @Test
        void testTotalInAccountCurrency_Success_ZeroFee() {

            List<Fee> fees = List.of();
            Money actualTotal = Fee.totalInAccountCurrency(fees, Currency.USD);
            Money actualTotal2 = Fee.totalInAccountCurrency(null, Currency.USD);
            assertEquals(Money.of(0, "USD"), actualTotal);
            assertEquals(Money.of(0, "USD"), actualTotal2);
        }

        @Test
        void testTotalInAccountCurrency_Failure_AmountCurrencyNotEqualToAccountCurrency() {
            Fee fee_1 = Fee.of(VALID_TYPE, VALID_MONEY, NOW);
            Fee fee_2 = Fee.of(VALID_TYPE, VALID_MONEY, NOW);
            Fee fee_3 = Fee.of(VALID_TYPE, VALID_MONEY, NOW);

            List<Fee> fees = List.of(fee_1, fee_2, fee_3);
            assertThatThrownBy(() -> Fee.totalInAccountCurrency(fees, Currency.JPY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Fee currency mismatch");
        }
    }

    @Nested
    @DisplayName("FeeMetadata Operations")
    class MetadataTests {

        @Test
        @DisplayName("metadataConstructor_success_HandlesNullMap")
        void metadataConstructor_success_HandlesNullMap() {
            Fee.FeeMetadata meta = new Fee.FeeMetadata(null);
            assertThat(meta.values()).isNotNull();
            assertThat(meta.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("with_success_ImmutableAddition")
        void with_success_ImmutableAddition() {
            Fee.FeeMetadata initial = new Fee.FeeMetadata(Map.of("A", "1"));
            Fee.FeeMetadata updated = initial.with("B", "2");

            assertThat(initial.containsKey("B")).isFalse(); // Ensure immutability
            assertThat(updated.get("B")).isEqualTo("2");
            assertThat(updated.get("A")).isEqualTo("1");
        }

        @Test
        @DisplayName("withAll_success_MergeMaps")
        void withAll_success_MergeMaps() {
            Fee.FeeMetadata initial = new Fee.FeeMetadata(Map.of("A", "1"));
            Map<String, String> extra = Map.of("B", "2", "C", "3");

            Fee.FeeMetadata merged = initial.withAll(extra);

            assertThat(merged.values()).hasSize(3).containsKeys("A", "B", "C");
        }

        @Test
        @DisplayName("getOrDefault_success_ReturnsFallback")
        void getOrDefault_success_ReturnsFallback() {
            Fee.FeeMetadata meta = new Fee.FeeMetadata(Map.of("exists", "yes"));
            assertThat(meta.getOrDefault("exists", "no")).isEqualTo("yes");
            assertThat(meta.getOrDefault("missing", "fallback")).isEqualTo("fallback");
        }
    }
}