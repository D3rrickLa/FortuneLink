package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidQuantityException;

import static org.assertj.core.api.Assertions.*;

class FeeTest {

    // --- Helper ---
    private static Money money(double amount) {
        return Money.of(BigDecimal.valueOf(amount), "USD");
    }

    // --- Canonical Constructor Tests ---
    @Test
    void constructor_shouldCreateFee() {
        Instant now = Instant.now();
        Fee fee = new Fee(FeeType.BROKERAGE, money(10), "Brokerage fee", now);

        assertThat(fee.type()).isEqualTo(FeeType.BROKERAGE);
        assertThat(fee.amount().amount()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(fee.description()).isEqualTo("Brokerage fee");
        assertThat(fee.time()).isEqualTo(now);
    }

    @Test
    void constructor_shouldAllowZeroAmount() {
        Fee fee = new Fee(FeeType.TRANSACTION_FEE, money(0), "Free fee", Instant.now());
        assertThat(fee.amount().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void constructor_shouldThrowForNegativeAmount() {
        assertThatExceptionOfType(InvalidQuantityException.class)
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(-1), "Negative fee", Instant.now()))
                .withMessage("Fee amount cannot be negative.");
    }

    @Test
    void constructor_shouldThrowForNullParameters() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fee(null, money(1), "Desc", Instant.now()))
                .withMessageContaining("Type cannot be null");

        assertThatNullPointerException()
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, null, "Desc", Instant.now()))
                .withMessageContaining("Amount cannot be null");

        assertThatNullPointerException()
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(1), null, Instant.now()))
                .withMessageContaining("Description cannot be null");

        assertThatNullPointerException()
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(1), "Desc", null))
                .withMessageContaining("Time cannot be null");
    }

    @Test
    void constructor_shouldThrowForBlankDescription() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(1), " ", Instant.now()))
                .withMessage("Description cannot be blank.");
    }

    // --- Builder Tests ---
    @Test
    void builder_shouldCreateFeeWithAllFields() {
        Instant now = Instant.now();
        Fee fee = Fee.builder()
                .type(FeeType.COMMISSION)
                .amount(money(5))
                .description("Commission fee")
                .time(now)
                .build();

        assertThat(fee.type()).isEqualTo(FeeType.COMMISSION);
        assertThat(fee.amount().amount()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(fee.description()).isEqualTo("Commission fee");
        assertThat(fee.time()).isEqualTo(now);
    }

    @Test
    void builder_shouldSetDefaultTime() {
        Fee fee = Fee.builder()
                .type(FeeType.COMMISSION)
                .amount(money(5))
                .description("Default time fee")
                .build();

        assertThat(fee.time()).isNotNull();
        assertThat(fee.time()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void builder_shouldEnforceValidation() {
        assertThatExceptionOfType(InvalidQuantityException.class)
                .isThrownBy(() -> Fee.builder()
                        .type(FeeType.COMMISSION)
                        .amount(money(-1))
                        .description("Negative fee")
                        .build());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Fee.builder()
                        .type(FeeType.COMMISSION)
                        .amount(money(1))
                        .description(" ")
                        .build());
    }

    @Test
    void builder_shouldThrowForNulls() {
        assertThatNullPointerException()
                .isThrownBy(() -> Fee.builder().type(null).amount(money(1)).description("desc").build());

        assertThatNullPointerException()
                .isThrownBy(() -> Fee.builder().type(FeeType.BROKERAGE).amount(null).description("desc").build());

        assertThatNullPointerException()
                .isThrownBy(() -> Fee.builder().type(FeeType.BROKERAGE).amount(money(1)).description(null).build());

        assertThatNullPointerException()
                .isThrownBy(() -> Fee.builder().type(FeeType.BROKERAGE).amount(money(1)).description("desc").time(null).build());
    }
}