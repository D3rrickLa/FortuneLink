package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.PriceResponse;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@DisplayName("MarketDataMapper Unit Tests")
class MarketDataMapperTest {

    private MarketDataMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MarketDataMapper();
    }

    @Test
    @DisplayName("toMoney: should map valid quote to domain Money object")
    void toMoney_ValidQuote_MapsCorrectly() {
        // Given
        ProviderQuote quote = new ProviderQuote("AAPL", new BigDecimal("150.00"), "USD", LocalDateTime.now(), "YAHOO");

        // When
        Money result = mapper.toMoney(quote);

        // Then
        assertThat(result.amount()).isEqualByComparingTo("150.00");
        assertThat(result.currency()).isEqualTo(ValidatedCurrency.USD);
    }

    @ParameterizedTest
    @CsvSource({
            "INVALID, USD", // Test unknown currency fallback
            "cad, CAD", // Test case insensitivity
            "USD, USD" // Test standard case
    })
    @DisplayName("toMoney: should handle currency resolution from valid records")
    void toMoney_CurrencyResolution_ReturnsExpected(String inputCurrency, ValidatedCurrency expected) {
        // These inputs are valid strings, so ProviderQuote won't complain
        ProviderQuote quote = new ProviderQuote("AAPL", BigDecimal.TEN, inputCurrency, LocalDateTime.now(), "TEST");

        Money result = mapper.toMoney(quote);

        assertThat(result.currency()).isEqualTo(expected);
    }

    @Test
    @DisplayName("toMoney: should handle currency resolution from valid records 2")
    public void toMoney_returnsSucess_whenNullPass() {
        // These inputs are valid strings, so ProviderQuote won't complain
        ProviderQuote quote = mock();

        when(quote.currency()).thenReturn(" ");
        when(quote.price()).thenReturn(BigDecimal.TEN);
        Money result = mapper.toMoney(quote);

        assertThat(result.currency()).isEqualTo(ValidatedCurrency.USD);
    }

    @ParameterizedTest
    @CsvSource({
            "INVALID, USD", // Test unknown currency fallback
            "cad, CAD", // Test case insensitivity
            "USD, USD" // Test standard case
    })
    @DisplayName("toMoney: should handle currency edge cases and fallbacks")
    void toMoney_CurrencyEdgeCases_ReturnsExpected(String inputCurrency, ValidatedCurrency expected) {
        // Use "NULL" string to represent actual null for CsvSource
        String currencyStr = "NULL".equals(inputCurrency) ? null : inputCurrency;
        ProviderQuote quote = new ProviderQuote("AAPL", BigDecimal.TEN, currencyStr, LocalDateTime.now(), "TEST");

        Money result = mapper.toMoney(quote);

        assertThat(result.currency()).isEqualTo(expected);
    }

    @Test
    @DisplayName("toAssetInfo: should map provider info and normalize symbol")
    void toAssetInfo_ValidInfo_MapsAndNormalizes() {
        // Given
        ProviderAssetInfo providerInfo = new ProviderAssetInfo(
                " tsla ", // Messy symbol
                "Tesla Inc",
                "Technology",
                "STOCK",
                "NASDAQ",
                "USD",
                "Automotive",
                "Description");

        // When
        MarketAssetInfo result = mapper.toAssetInfo(providerInfo);

        // Then
        assertThat(result.getSymbol()).isEqualTo("TSLA"); // Normalized
        assertThat(result.getName()).isEqualTo("Tesla Inc");
        assertThat(result.getAssetType()).isEqualTo(AssetType.STOCK);
    }

    @Test
    @DisplayName("normalizeSymbol: should trim and uppercase strings")
    void normalizeSymbol_Success() {
        assertThat(mapper.normalizeSymbol(" vgro.to ")).isEqualTo("VGRO.TO");
        assertThat(mapper.normalizeSymbol("btc-usd")).isEqualTo("BTC-USD");
    }

    @Test
    @DisplayName("normalizeSymbol: should throw exception for null")
    void normalizeSymbol_Null_ThrowsException() {
        assertThatThrownBy(() -> mapper.normalizeSymbol(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toProviderSymbol: should return primary ID for Yahoo")
    void toProviderSymbol_Yahoo_ReturnsIdentity() {
        AssetIdentifier id = mock(AssetIdentifier.class);
        when(id.getPrimaryId()).thenReturn("AAPL");

        String result = mapper.toProviderSymbol(id, "YAHOO_FINANCE");

        assertThat(result).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("toProviderSymbol: should return primary ID for Alpha")
    void toProviderSymbol_Alpha_ReturnsIdentity() {
        AssetIdentifier id = mock(AssetIdentifier.class);
        when(id.getPrimaryId()).thenReturn("AAPL");

        String result = mapper.toProviderSymbol(id, "ALPGA");

        assertThat(result).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("toPriceResponse: should map domain Money to response DTO")
    void toPriceResponse_MapsCorrectly() {
        Money price = new Money(new BigDecimal("100.00"), ValidatedCurrency.USD);

        PriceResponse response = mapper.toPriceResponse("AAPL", price);

        assertThat(response.getSymbol()).isEqualTo("AAPL");
        assertThat(response.getPrice()).isEqualByComparingTo("100.00");
        assertThat(response.getCurrency()).isEqualTo("USD"); // Assuming USD.getSymbol() is $
    }
}