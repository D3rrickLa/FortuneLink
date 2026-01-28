package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketAssetQuoteTest {

    private final AssetIdentifier id = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
    private final Money hundredUsd = Money.of(new BigDecimal("100.00"), ValidatedCurrency.USD);
    private final Instant now = Instant.now();

    @Nested
    @DisplayName("Constructor Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when required fields are null")
        void shouldThrowExceptionWhenRequiredFieldsAreNull() {
            assertThatThrownBy(() -> new MarketAssetQuote(null, hundredUsd, null, null, null, null, null, null, null,
                    null, now, "TEST"))
                    .isInstanceOf(NullPointerException.class).hasMessageContaining("AssetIdentifier");

            assertThatThrownBy(
                    () -> new MarketAssetQuote(id, null, null, null, null, null, null, null, null, null, now, "TEST"))
                    .isInstanceOf(NullPointerException.class).hasMessageContaining("Current price");

            assertThatThrownBy(() -> new MarketAssetQuote(id, hundredUsd, null, null, null, null, null, null, null,
                    null, null, "TEST"))
                    .isInstanceOf(NullPointerException.class).hasMessageContaining("Last updated");
        }
    }

    @Nested
    @DisplayName("computeChangeAmount Tests")
    class ComputeChangeAmountTests {

        @Test
        @DisplayName("Should return null when previousClose is missing")
        void shouldReturnNullWhenPreviousCloseIsNull() {
            MarketAssetQuote quote = createQuote(hundredUsd, null);
            assertThat(quote.computeChangeAmount()).isNull();
        }

        @Test
        @DisplayName("Should calculate positive and negative price change correctly")
        void shouldCalculateChangeCorrectly() {
            // Case: Gain (100 - 80 = 20)
            MarketAssetQuote gain = createQuote(hundredUsd, Money.of(new BigDecimal("80.00"), ValidatedCurrency.USD));
            assertThat(gain.computeChangeAmount()).isEqualByComparingTo("20.00");

            // Case: Loss (100 - 120 = -20)
            MarketAssetQuote loss = createQuote(hundredUsd, Money.of(new BigDecimal("120.00"), ValidatedCurrency.USD));
            assertThat(loss.computeChangeAmount()).isEqualByComparingTo("-20.00");
        }
    }

    @Nested
    @DisplayName("computeChangePercent Tests")
    class ComputeChangePercentTests {

        @Test
        @DisplayName("Should return null when previousClose is null or zero")
        void shouldReturnNullOnInvalidPreviousClose() {
            // Null branch
            assertThat(createQuote(hundredUsd, null).computeChangePercent()).isNull();

            // Zero branch (prevents ArithmeticException)
            MarketAssetQuote zeroClose = createQuote(hundredUsd, Money.of(BigDecimal.ZERO, ValidatedCurrency.USD));
            assertThat(zeroClose.computeChangePercent()).isNull();
        }

        @ParameterizedTest
        @CsvSource({
                "110.00, 100.00, 10.00", // 10% gain
                "90.00, 100.00, -10.00", // 10% loss
                "100.00, 100.00, 0.00", // No change
                "150.00, 100.00, 50.00" // 50% gain
        })
        @DisplayName("Should calculate percentage change correctly")
        void shouldCalculatePercentageChange(String current, String prev, String expected) {
            MarketAssetQuote quote = createQuote(
                    Money.of(new BigDecimal(current), ValidatedCurrency.USD),
                    Money.of(new BigDecimal(prev), ValidatedCurrency.USD));

            assertThat(quote.computeChangePercent()).isEqualByComparingTo(expected);
        }
    }

    @Nested
    @DisplayName("fromProvider Factory Tests")
    class FactoryTests {

        @Test
        @DisplayName("Should map ProviderQuote to MarketAssetQuote correctly")
        void shouldMapFromProvider() {
            // Arrange
            LocalDateTime ldt = LocalDateTime.of(2024, 5, 20, 10, 0);
            BigDecimal price = new BigDecimal("175.50");
            BigDecimal changePct = new BigDecimal("2.5");
            BigDecimal mktCap = new BigDecimal("3000000000000");

            // Correcting parameter order:
            // Assuming ProviderQuote(symbol, price, currency, timestamp, changePct,
            // marketCap, source)
            ProviderQuote providerQuote = new ProviderQuote(
                    "AAPL",
                    price,
                    "USD",
                    ldt,
                    mktCap, // marketCap correctly positioned
                    changePct, // changePercent correctly positioned
                    "YAHOO");

            // Act
            MarketAssetQuote result = MarketAssetQuote.fromProvider(id, providerQuote);

            // Assert
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.currentPrice().amount()).isEqualByComparingTo(price);
            assertThat(result.changePercent()).isEqualByComparingTo(changePct);
            assertThat(result.marketCap()).isEqualByComparingTo(mktCap);
            assertThat(result.source()).isEqualTo("YAHOO");

            // Verify the ACL (Anti-Corruption Layer) logic:
            // It should convert LocalDateTime to Instant at UTC
            assertThat(result.lastUpdated()).isEqualTo(ldt.toInstant(java.time.ZoneOffset.UTC));
        }
    }

    // Helper to reduce boilerplate
    private MarketAssetQuote createQuote(Money current, Money previous) {
        return new MarketAssetQuote(
                id, current, null, null, null, previous,
                null, null, null, null, now, "TEST");
    }
}