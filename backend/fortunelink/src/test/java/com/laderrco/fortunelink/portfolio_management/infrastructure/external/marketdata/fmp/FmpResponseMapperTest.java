package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpQuoteResponse;

class FmpResponseMapperTest {

    private FmpResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FmpResponseMapper();
    }

    @Test
    @DisplayName("toProviderQuote: should map FMP quote response correctly")
    void toProviderQuote_ShouldMapCorrectly() {
        // Given
        long epochSeconds = LocalDateTime.of(2024, 5, 20, 10, 0).toEpochSecond(ZoneOffset.UTC);
        FmpQuoteResponse response = mock(FmpQuoteResponse.class);
        when(response.getSymbol()).thenReturn("AAPL");
        when(response.getPrice()).thenReturn(new BigDecimal("185.50"));
        when(response.getExchange()).thenReturn("NASDAQ");
        when(response.getTimestamp()).thenReturn(epochSeconds);

        // When
        ProviderQuote result = mapper.toProviderQuote(response);

        // Then
        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.price()).isEqualByComparingTo("185.50");
        assertThat(result.currency()).isEqualTo("USD"); // Inferred from NASDAQ
        assertThat(result.source()).isEqualTo("Financial Modeling Prep");
    }

    @ParameterizedTest
    @CsvSource({
            "true,  false, Stock, STOCK, ETF",
            "false, true,  Stock, STOCK, MUTUAL_FUND",
            "false, false, Index, INDEX, INDEX",
            "false, false, Tech,  CRYPTO, CRYPTO",
            "false, false, Tech,  NASDAQ, STOCK"
    })
    @DisplayName("toProviderAssetInfo: should infer correct asset types")
    void toProviderAssetInfo_ShouldInferCorrectAssetType(
            boolean isEtf, boolean isFund, String industry, String exchange, String expectedType) {

        // Given
        FmpProfileResponse response = mock(FmpProfileResponse.class);
        when(response.getSymbol()).thenReturn("TEST");
        when(response.getIsEtf()).thenReturn(isEtf);
        when(response.getIsFund()).thenReturn(isFund);
        when(response.getIndustry()).thenReturn(industry);
        when(response.getExchange()).thenReturn(exchange);

        // When
        ProviderAssetInfo result = mapper.toProviderAssetInfo(response);

        // Then
        assertThat(result.assetType()).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @CsvSource({
            "TSX,     CAD",
            "LSE,     GBP",
            "XETRA,   EUR",
            "HKSE,    HKD",
            "CRYPTO,  USD",
            "FOREX,  USD",
            "COMMODITY,  USD",
            "INDEX,  USD",
            "INVALID, USD"
    })
    @DisplayName("inferFromExchange: should map exchanges to correct currencies")
    void inferFromExchange_ShouldMapCorrectly(String exchange, String expectedCurrency) {
        // Given
        FmpQuoteResponse response = mock(FmpQuoteResponse.class);
        when(response.getSymbol()).thenReturn("SYMBOL");
        when(response.getPrice()).thenReturn(BigDecimal.TEN);
        when(response.getExchange()).thenReturn(exchange);
        when(response.getTimestamp()).thenReturn(1700000000L);

        // When
        ProviderQuote result = mapper.toProviderQuote(response);

        // Then
        assertThat(result.currency()).isEqualTo(expectedCurrency);
    }

    @Test
    @DisplayName("toProviderQuote: should handle null exchange gracefully")
    void toProviderQuote_HandleNullExchange() {
        FmpQuoteResponse response = mock(FmpQuoteResponse.class);
        when(response.getSymbol()).thenReturn("AAPL");
        when(response.getPrice()).thenReturn(BigDecimal.ONE);
        when(response.getExchange()).thenReturn(null);

        ProviderQuote result = mapper.toProviderQuote(response);

        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("mapQuoteTypeToAssetType: Should identify CURRENCY from exchange string")
    void mapQuoteTypeToAssetType_ForexDetection() {
        // Given
        FmpProfileResponse response = mock(FmpProfileResponse.class);

        // Mandatory fields for ProviderAssetInfo Record
        when(response.getSymbol()).thenReturn("EURUSD");
        when(response.getCompanyName()).thenReturn("Euro / US Dollar");
        when(response.getCurrency()).thenReturn("USD");

        // Target Logic fields
        when(response.getIsEtf()).thenReturn(false);
        when(response.getIsFund()).thenReturn(false);
        when(response.getExchange()).thenReturn("FOREX");

        // When
        ProviderAssetInfo result = mapper.toProviderAssetInfo(response);

        // Then
        assertThat(result.assetType()).isEqualTo("CURRENCY");
    }

    @Test
    @DisplayName("mapQuoteTypeToAssetType: Should NOT identify CURRENCY from exchange string")
    void mapQuoteTypeToAssetType_ForexDetectionExchagneNULL() {
        // Given
        FmpProfileResponse response = mock(FmpProfileResponse.class);

        // Mandatory fields for ProviderAssetInfo Record
        when(response.getSymbol()).thenReturn("EURUSD");
        when(response.getCompanyName()).thenReturn("Euro / US Dollar");
        when(response.getCurrency()).thenReturn("USD");

        // Target Logic fields
        when(response.getIsEtf()).thenReturn(false);
        when(response.getIsFund()).thenReturn(false);
        when(response.getExchange()).thenReturn(null);

        // When
        ProviderAssetInfo result = mapper.toProviderAssetInfo(response);

        // Then
        assertThat(result.assetType()).isEqualTo("STOCK");
    }

    @Test
    @DisplayName("mapQuoteTypeToAssetType: Should identify CURRENCY from exchange string")
    void mapQuoteTypeToAssetType_ForexDetectionForCURRENCY() {
        // Given
        FmpProfileResponse response = mock(FmpProfileResponse.class);

        // Mandatory fields for ProviderAssetInfo Record
        when(response.getSymbol()).thenReturn("EURUSD");
        when(response.getCompanyName()).thenReturn("Euro / US Dollar");
        when(response.getCurrency()).thenReturn("USD");

        // Target Logic fields
        when(response.getIsEtf()).thenReturn(false);
        when(response.getIsFund()).thenReturn(false);
        when(response.getExchange()).thenReturn("CURRENCY");

        // When
        ProviderAssetInfo result = mapper.toProviderAssetInfo(response);

        // Then
        assertThat(result.assetType()).isEqualTo("CURRENCY");
    }

    @Test
    @DisplayName("mapQuoteTypeToAssetType: Should NOT identify CURRENCY from exchange string")
    void mapQuoteTypeToAssetType_ForexDetectionNotDetected() {
        // Given
        FmpProfileResponse response = mock(FmpProfileResponse.class);
        
        // Mandatory fields for ProviderAssetInfo Record
        when(response.getSymbol()).thenReturn("EURUSD");
        when(response.getCompanyName()).thenReturn("Euro / US Dollar");
        when(response.getCurrency()).thenReturn("USD");
        
        // Target Logic fields
        when(response.getIsEtf()).thenReturn(false);
        when(response.getIsFund()).thenReturn(false);
        when(response.getExchange()).thenReturn("Foreign Exchange");

        // When
        ProviderAssetInfo result = mapper.toProviderAssetInfo(response);

        // Then
        assertThat(result.assetType()).isEqualTo("STOCK");
    }

    @Test
    @DisplayName("mapQuoteTypeToAssetType: Should identify INDEX from industry string")
    void mapQuoteTypeToAssetType_IndustryIndexDetection() {
        // Given
        FmpProfileResponse response = mock(FmpProfileResponse.class);

        // Mandatory fields
        when(response.getSymbol()).thenReturn("^GSPC");
        when(response.getCompanyName()).thenReturn("S&P 500");
        when(response.getCurrency()).thenReturn("USD");

        // Target Logic fields
        when(response.getIsEtf()).thenReturn(false);
        when(response.getIsFund()).thenReturn(false);
        when(response.getExchange()).thenReturn("NASDAQ");
        when(response.getIndustry()).thenReturn("World Market Index");

        // When
        ProviderAssetInfo result = mapper.toProviderAssetInfo(response);

        // Then
        assertThat(result.assetType()).isEqualTo("INDEX");
    }

    @ParameterizedTest
    @CsvSource({
            "ASX, AUD",
            "NSE, INR",
            "JPX, JPY"
    })

    @DisplayName("inferFromExchange: Should map specific global exchanges to correct currencies")
    void inferFromExchange_SpecificGlobalExchanges(String exchange, String expectedCurrency) {
        // Given
        FmpQuoteResponse response = mock(FmpQuoteResponse.class);
        when(response.getSymbol()).thenReturn("TEST");
        when(response.getPrice()).thenReturn(BigDecimal.TEN);
        when(response.getTimestamp()).thenReturn(1700000000L);
        // This triggers the specific switch arms for ASX, NSE, and JPX
        when(response.getExchange()).thenReturn(exchange);

        // When
        ProviderQuote result = mapper.toProviderQuote(response);

        // Then
        assertThat(result.currency()).isEqualTo(expectedCurrency);
    }
}