package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeCategory;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.*;

public class FeeTest {
    // Mock/Stub data for the required parameters
    private final FeeType mockType = FeeType.ACCOUNT_MAINTENANCE; // Replace with your actual enum/class
    private final Money mockAmount = new Money(BigDecimal.valueOf(10.0), ValidatedCurrency.USD); 
    private final ExchangeRate mockRate = new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.USD, BigDecimal.valueOf(1), Instant.now(), null);
    private final Instant mockDate = Instant.now();

    // --- Canonical Constructor Tests ---
    @Test
    void constructor_shouldCreateFee() {
        Instant now = Instant.now();
        Fee fee = new Fee(FeeType.BROKERAGE, money(10),  exchangeRate(), Map.of("Description", "Brokerage fee"), now);

        assertThat(fee.feeType().equals(FeeType.BROKERAGE));
        assertThat(fee.amountInNativeCurrency().amount().equals(BigDecimal.valueOf(10)));
        assertThat(fee.metadata().get("Description")).isEqualTo("Brokerage fee");
        assertThat(fee.feeDate()).isEqualTo(now);
        assertThat(fee.feeType().getCategory()).isEqualTo(FeeCategory.TRADING);
    }

    @Test
    void constructor_shouldAllowZeroAmount() {
        Fee fee = new Fee(FeeType.TRANSACTION_FEE, money(0), exchangeRate(), null, Instant.now());
        assertThat(fee.amountInNativeCurrency().amount()).isEqualTo(BigDecimal.ZERO.setScale(Precision.getMoneyPrecision()));
    }

    @Test
    void constructor_shouldThrowForNegativeAmount() {
        assertThatExceptionOfType(InvalidQuantityException.class)
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(-1), exchangeRate(), Collections.emptyMap(), Instant.now()))
                .withMessage("Fee amount cannot be negative.");
    }

    @Test
    void constructor_shouldThrowForNullParameters() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fee(null, money(1), exchangeRate(), Collections.emptyMap(), Instant.now()))
                .withMessageContaining("Fee Type cannot be null");

        assertThatNullPointerException()
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, null, exchangeRate(),  Collections.emptyMap(), Instant.now()))
                .withMessageContaining("Amount In Native Currency cannot be null");

        // assertThatNullPointerException()
        //         .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(1), null, Collections.emptyMap(), Instant.now()))
        //         .withMessageContaining("Description cannot be null");

        assertThatNullPointerException()
                .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(1), exchangeRate(), Collections.emptyMap(), null))
                .withMessageContaining("Fee Date cannot be null");
    }
    // testing the branch in the canonical constructor
    @Test
    @DisplayName("Should throw exception when metadata contains a blank key")
    void constructor_WithBlankMetadataKey_ThrowsException() {
        Map<String, String> invalidMetadata = Map.of(" ", "validValue");

        assertThatThrownBy(() -> 
            new Fee(mockType, mockAmount, mockRate, invalidMetadata, mockDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Metadata has empty properties in it");
    }

    @Test
    @DisplayName("Should throw exception when metadata contains a blank value")
    void constructor_WithBlankMetadataValue_ThrowsException() {
        Map<String, String> invalidMetadata = Map.of("validKey", "");

        assertThatThrownBy(() -> 
            new Fee(mockType, mockAmount, mockRate, invalidMetadata, mockDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Metadata has empty properties in it");
    }

    @Test
    @DisplayName("Should handle null metadata by converting it to an empty unmodifiable map")
    void constructor_WithNullMetadata_SetsEmptyMap() {
        Fee fee = new Fee(mockType, mockAmount, mockRate, null, mockDate);

        assertThat(fee.metadata()).isEmpty();
        // Verify it is unmodifiable
        assertThatThrownBy(() -> fee.metadata().put("new", "key"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should make valid metadata unmodifiable")
    void constructor_WithValidMetadata_MakesItUnmodifiable() {
        Map<String, String> mutableMetadata = new HashMap<>();
        mutableMetadata.put("source", "web");

        Fee fee = new Fee(mockType, mockAmount, mockRate, mutableMetadata, mockDate);

        assertThat(fee.metadata()).containsEntry("source", "web");
        
        // Ensure the record holds a copy or unmodifiable view, not the original mutable map
        assertThatThrownBy(() -> fee.metadata().put("fail", "here"))
            .isInstanceOf(UnsupportedOperationException.class);
    }    

    @Test
    @DisplayName("Path 1: Metadata is null (skips loop)")
    void constructor_NullMetadata_ShouldNotThrow() {
        // This triggers: metadata != null -> FALSE
        Fee fee = new Fee(mockType, mockAmount, mockRate, null, mockDate);
        assertThat(fee.metadata()).isEmpty();
    }

    @Test
    @DisplayName("Path 2: Metadata is empty (skips loop)")
    void constructor_EmptyMetadata_ShouldNotThrow() {
        // This triggers: metadata != null -> TRUE, !metadata.isEmpty() -> FALSE
        Fee fee = new Fee(mockType, mockAmount, mockRate, Map.of(), mockDate);
        assertThat(fee.metadata()).isEmpty();
    }

    @Test
    @DisplayName("Path 3: Metadata is blank (enters loop and fails)")
    void constructor_BlankMetadata_ShouldThrow() {
        // This triggers: metadata != null -> TRUE, !metadata.isEmpty() -> TRUE
        Map<String, String> brokenMap = Map.of(" ", "value");
        
        assertThatThrownBy(() -> 
            new Fee(mockType, mockAmount, mockRate, brokenMap, mockDate))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // @Test
    // void constructor_shouldThrowForBlankDescription() {
    //     assertThatIllegalArgumentException()
    //             .isThrownBy(() -> new Fee(FeeType.BROKERAGE, money(1), exchangeRate(), Collections.emptyMap(), Instant.now()))
    //             .withMessage("Description cannot be blank.");
    // }

    // --- Builder Tests ---
    @Test
    void builder_shouldCreateFeeWithAllFields() {
        Instant now = Instant.now();
        Fee fee = Fee.builder()
                .feeType(FeeType.COMMISSION)
                .amountInNativeCurrency(money(5))
                .metadata(Map.of("Description", "Commission fee"))
                .exchangeRate(exchangeRate())
                .feeDate(now)
                .build();

        assertThat(fee.feeType()).isEqualTo(FeeType.COMMISSION);
        assertThat(fee.amountInNativeCurrency().amount()).isEqualTo(BigDecimal.valueOf(5).setScale(Precision.getMoneyPrecision()));
        assertThat(fee.metadata().get("Description")).isEqualTo("Commission fee");
        assertThat(fee.feeDate()).isEqualTo(now);
    }

    @Test
    void builder_shouldSetDefaultTime() {
        Fee fee = Fee.builder()
                .feeType(FeeType.COMMISSION)
                .amountInNativeCurrency(money(5))
                .metadata(Map.of("Description","Default time fee"))
                .exchangeRate(exchangeRate())
                .feeDate(Instant.now())
                .build();

        assertThat(fee.feeDate()).isNotNull();
        assertThat(fee.feeDate()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void builder_shouldEnforceValidation() {
        assertThatExceptionOfType(InvalidQuantityException.class)
                .isThrownBy(() -> Fee.builder()
                        .feeType(FeeType.COMMISSION)
                        .amountInNativeCurrency(money(-1))
                        .metadata(Map.of("Description","Negative fee"))
                        .exchangeRate(exchangeRate())
                        .feeDate(Instant.now())
                        .build());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Fee.builder()
                        .feeType(FeeType.COMMISSION)
                        .amountInNativeCurrency(money(1))
                        .metadata(Map.of(" ", " "))
                        .exchangeRate(exchangeRate())
                        .feeDate(Instant.now())
                        .build());
    }

    @Test
    void builder_shouldThrowForNulls() {
        assertThatNullPointerException()
                .isThrownBy(() -> Fee.builder().feeType(null).amountInNativeCurrency(money(1)).metadata(Map.of("Description","Negative fee")).build());

        assertThatNullPointerException()
                .isThrownBy(() -> Fee.builder().feeType(FeeType.BROKERAGE).amountInNativeCurrency(null).metadata(Map.of("Description","Negative fee")).build());

        // assertThatNullPointerException()
        //         .isThrownBy(() -> Fee.builder().feeType(FeeType.BROKERAGE).amountInNativeCurrency(money(1)).description(null).build());

        assertThatNullPointerException()
                .isThrownBy(() -> Fee.builder().feeType(FeeType.BROKERAGE).amountInNativeCurrency(money(1)).metadata(Map.of("Description","Negative fee")).feeDate(null).build());
    }

            // --- Helper ---
    private static Money money(double amount) {
        Money ma = Money.of(amount, "USD");
        return ma;
    }

    private static ExchangeRate exchangeRate() {
        ExchangeRate conversion = ExchangeRate.create("USD", "CAD", 1.42, Instant.now(), null);
        return conversion;
    }
    
    // --- toBaseCurrency Tests ---
    @Test
    void toBaseCurrency_shouldReturnSameMoneyWhenCurrenciesMatch() {
        Money amount = money(10);
        Fee fee = new Fee(FeeType.BROKERAGE, amount, exchangeRate(), null, Instant.now());
        
        Money result = fee.toBaseCurrency(amount.currency());
        
        assertThat(result).isEqualTo(amount);
    }
    
    @Test
    void toBaseCurrency_shouldConvertWhenCurrenciesDiffer() {
        Money amount = Money.of(10, "USD");
        ExchangeRate rate = ExchangeRate.create("USD", "CAD", 1.42, Instant.now(), null);
        Fee fee = new Fee(FeeType.BROKERAGE, amount, rate, null, Instant.now());
        
        Money result = fee.toBaseCurrency(rate.to());
        
        assertThat(result).isNotNull();
        assertThat(result.currency().getCode()).isEqualTo("CAD");
    }
    
    @Test
    void toBaseCurrency_shouldThrowWhenExchangeRateDoesNotMatch() {
        Money amount = Money.of(10, "USD");
        ExchangeRate rate = ExchangeRate.create("USD", "CAD", 1.42, Instant.now(), null);
        Fee fee = new Fee(FeeType.BROKERAGE, amount, rate, null, Instant.now());
        
        assertThatExceptionOfType(CurrencyMismatchException.class)
                .isThrownBy(() -> fee.toBaseCurrency(ValidatedCurrency.EUR));
    }
    
    // --- apply Tests ---
    @Test
    void apply_shouldSubtractFeeWhenCurrenciesMatch() {
        Money fee = money(5);
        Money baseAmount = money(100);
        Fee feeObj = new Fee(FeeType.BROKERAGE, fee, exchangeRate(), null, Instant.now());
        
        Money result = feeObj.apply(baseAmount);
        
        assertThat(result.amount()).isEqualTo(BigDecimal.valueOf(95).setScale(Precision.getMoneyPrecision()));
    }
    
    @Test
    void apply_shouldConvertAndSubtractWhenCurrenciesDiffer() {
        Money feeAmount = Money.of(10, "USD");
        ExchangeRate rate = ExchangeRate.create("USD", "CAD", 1.42, Instant.now(), null);
        Money baseAmount = Money.of(100, "CAD");
        Fee fee = new Fee(FeeType.BROKERAGE, feeAmount, rate, null, Instant.now());
        
        Money result = fee.apply(baseAmount);
        
        assertThat(result).isNotNull();
        assertThat(result.currency().getCode()).isEqualTo("CAD");
    }
    
    @Test
    void apply_shouldThrowWhenCurrencyMismatchAndNoExchangeRate() {
        Money feeAmount = Money.of(10, "USD");
        ExchangeRate rate = ExchangeRate.create("EUR", "GBP", 1.10, Instant.now(), null);
        Money baseAmount = Money.of(100, "CAD");
        Fee fee = new Fee(FeeType.BROKERAGE, feeAmount, rate, null, Instant.now());
        
        assertThatExceptionOfType(CurrencyMismatchException.class)
                .isThrownBy(() -> fee.apply(baseAmount));
    }
    
    @Test
    void apply_shouldThrowForNullBaseAmount() {
        Fee fee = new Fee(FeeType.BROKERAGE, money(5), exchangeRate(), null, Instant.now());
        
        assertThatNullPointerException()
                .isThrownBy(() -> fee.apply(null));
    }
}

