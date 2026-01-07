package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.ErrorType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.mappers.MarketDataMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.providers.MarketDataProvider;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Unit Tests")
class MarketDataServiceImplTest {

    @Mock
    private MarketDataProvider provider;

    @Mock
    private MarketDataMapper mapper;

    private MarketDataServiceImpl service;

    private MarketIdentifier testIdentifier;
    private String Id;
    private AssetType assetType;
    private String name;
    private String uOt;

    @BeforeEach
    void setUp() {
        service = new MarketDataServiceImpl(provider, mapper);

        Id = "AAPL";
        assetType = AssetType.STOCK;
        name = "Apple";
        uOt = "$US";
        testIdentifier = new MarketIdentifier(Id, null, assetType, name, uOt, null);
    }

    @Test
    @DisplayName("Should fetch current price successfully")
    void shouldFetchCurrentPrice() {
        // Given
        
        ProviderQuote quote = new ProviderQuote(
                "AAPL",
                new BigDecimal("150.00"),
                "USD",
                LocalDateTime.now(),
                "YAHOO");
        Money expectedPrice = new Money(new BigDecimal("150.00"), ValidatedCurrency.USD);

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(testIdentifier.getPrimaryId(), "YAHOO_FINANCE")).thenReturn("AAPL");
        when(provider.fetchCurrentQuote("AAPL")).thenReturn(Optional.of(quote));
        when(mapper.toMoney(quote)).thenReturn(expectedPrice);

        // When
        Money result = service.getCurrentPrice(testIdentifier);

        // Then
        assertThat(result).isEqualTo(expectedPrice);
        verify(provider).fetchCurrentQuote("AAPL");
        verify(mapper).toMoney(quote);
    }

    @Test
    @DisplayName("Should throw exception when symbol not found")
    void shouldThrowExceptionWhenSymbolNotFound() {

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(testIdentifier.getPrimaryId(), "YAHOO_FINANCE")).thenReturn("INVALID");
        when(provider.fetchCurrentQuote("INVALID")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getCurrentPrice(testIdentifier))
                .isInstanceOf(MarketDataException.class)
                .hasMessageContaining("INVALID")
                .extracting("errorType")
                .isEqualTo(ErrorType.SYMBOL_NOT_FOUND);
    }

    @Test
    @DisplayName("Should fetch historical price successfully")
    void shouldFetchHistoricalPrice() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 0);
        ProviderQuote quote = new ProviderQuote(
                "AAPL",
                new BigDecimal("145.00"),
                "USD",
                dateTime,
                "YAHOO");
        Money expectedPrice = new Money(new BigDecimal("145.00"), ValidatedCurrency.USD);

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(testIdentifier.getPrimaryId(), "YAHOO_FINANCE")).thenReturn("AAPL");
        when(provider.fetchHistoricalQuote("AAPL", dateTime)).thenReturn(Optional.of(quote));
        when(mapper.toMoney(quote)).thenReturn(expectedPrice);

        // When
        Money result = service.getHistoricalPrice(testIdentifier, dateTime);

        // Then
        assertThat(result).isEqualTo(expectedPrice);
        verify(provider).fetchHistoricalQuote("AAPL", dateTime);
    }

    @Test
    @DisplayName("Should throw exception when historical data unavailable")
    void shouldThrowExceptionWhenHistoricalDataUnavailable() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(1990, 1, 1, 0, 0);

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(testIdentifier, "YAHOO_FINANCE")).thenReturn("AAPL");
        when(provider.fetchHistoricalQuote("AAPL", dateTime)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getHistoricalPrice(testIdentifier, dateTime))
                .isInstanceOf(MarketDataException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.DATA_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should fetch batch prices successfully")
    void shouldFetchBatchPrices() {
        // Given
        List<AssetSymbol> symbols = List.of(
                new AssetSymbol("AAPL"),
                new AssetSymbol("GOOGL"),
                new AssetSymbol("MSFT"));

        Map<String, ProviderQuote> providerQuotes = Map.of(
                "AAPL", new ProviderQuote("AAPL", new BigDecimal("150"), "USD", LocalDateTime.now(), "YAHOO"),
                "GOOGL", new ProviderQuote("GOOGL", new BigDecimal("140"), "USD", LocalDateTime.now(), "YAHOO"),
                "MSFT", new ProviderQuote("MSFT", new BigDecimal("380"), "USD", LocalDateTime.now(), "YAHOO"));

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(any(), eq("YAHOO_FINANCE")))
                .thenAnswer(inv -> inv.getArgument(0, AssetSymbol.class).value());
        when(provider.fetchBatchQuotes(anyList())).thenReturn(providerQuotes);
        when(mapper.toMoney(any())).thenAnswer(inv -> {
            ProviderQuote q = inv.getArgument(0);
            return new Money(q.price(), ValidatedCurrency.USD);
        });

        // When
        Map<AssetIdentifier, Money> result = service.getBatchPrices(symbols);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(new AssetSymbol("AAPL")).amount())
                .isEqualByComparingTo(new BigDecimal("150"));
        verify(provider).fetchBatchQuotes(anyList());
    }

    @Test
    @DisplayName("Should handle empty batch request")
    void shouldHandleEmptyBatchRequest() {
        // When
        Map<AssetIdentifier, Money> result = service.getBatchPrices(Collections.emptyList());

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("Should fetch asset info successfully")
    void shouldFetchAssetInfo() {
        // Given
        AssetSymbol symbol = new AssetSymbol("AAPL");
        ProviderAssetInfo providerInfo = new ProviderAssetInfo(
                "AAPL",
                "Apple Inc.",
                "Technology company",
                "STOCK",
                "NASDAQ",
                "USD",
                "YAHOO");
        MarketDataService.AssetInfo expectedInfo = new MarketDataService.AssetInfo(
                symbol,
                "Apple Inc.",
                "Technology company",
                "STOCK",
                "NASDAQ",
                "USD");

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(symbol, "YAHOO_FINANCE")).thenReturn("AAPL");
        when(provider.fetchAssetInfo("AAPL")).thenReturn(Optional.of(providerInfo));
        when(mapper.toAssetInfo(providerInfo)).thenReturn(expectedInfo);

        // When
        Optional<MarketAssetInfo> result = service.getAssetInfo(symbol);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Apple Inc.");
        assertThat(result.get().getAssetType()).isEqualTo("STOCK");
    }

    @Test
    @DisplayName("Should return empty when asset info not found")
    void shouldReturnEmptyWhenAssetInfoNotFound() {
        // Given
        AssetSymbol symbol = new AssetSymbol("INVALID");

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(symbol, "YAHOO_FINANCE")).thenReturn("INVALID");
        when(provider.fetchAssetInfo("INVALID")).thenReturn(Optional.empty());

        // When
        Optional<MarketAssetInfo> result = service.getAssetInfo(symbol);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check symbol support")
    void shouldCheckSymbolSupport() {
        // Given
        AssetSymbol validSymbol = new AssetSymbol("AAPL");
        AssetSymbol invalidSymbol = new AssetSymbol("@INVALID");

        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(validSymbol, "YAHOO_FINANCE")).thenReturn("AAPL");
        when(mapper.toProviderSymbol(invalidSymbol, "YAHOO_FINANCE")).thenReturn("@INVALID");
        when(provider.supportSymbol("AAPL")).thenReturn(true);
        when(provider.supportSymbol("@INVALID")).thenReturn(false);

        // When/Then
        assertThat(service.isSymbolSupported(validSymbol)).isTrue();
        assertThat(service.isSymbolSupported(invalidSymbol)).isFalse();
    }
}