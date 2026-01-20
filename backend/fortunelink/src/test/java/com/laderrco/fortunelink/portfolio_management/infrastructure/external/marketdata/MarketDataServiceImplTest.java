package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
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
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataProvider;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.MarketDataMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
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

    @BeforeEach
    void setUp() {
        service = new MarketDataServiceImpl(provider, mapper);
        testIdentifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "$US", null);
    }

    @Test
    @DisplayName("Should fetch current price successfully")
    void shouldFetchCurrentPrice() {
        // Given
        ProviderQuote quote = createQuote("AAPL", "150.00");
        Money expectedPrice = new Money(new BigDecimal("150.00"), ValidatedCurrency.USD);

        // Logic check: Implementation calls
        // provider.fetchCurrentQuote(assetIdentifier.getPrimaryId())
        when(provider.fetchCurrentQuote("AAPL")).thenReturn(Optional.of(quote));
        when(mapper.toMoney(quote)).thenReturn(expectedPrice);

        // When
        Money result = service.getCurrentPrice(testIdentifier);

        // Then
        assertThat(result).isEqualTo(expectedPrice);
        verify(provider).fetchCurrentQuote("AAPL");
    }

    @Test
    @DisplayName("Should throw exception when symbol not found")
    void shouldThrowExceptionWhenSymbolNotFound() {
        // Given
        when(provider.fetchCurrentQuote("AAPL")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getCurrentPrice(testIdentifier))
                .isInstanceOf(MarketDataException.class)
                .hasMessageContaining("AAPL");
    }

    @Test
    @DisplayName("Should fetch historical price successfully")
    void shouldFetchHistoricalPrice() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 0);
        ProviderQuote quote = createQuote("AAPL", "145.00");
        Money expectedPrice = new Money(new BigDecimal("145.00"), ValidatedCurrency.USD);

        when(provider.fetchHistoricalQuote("AAPL", dateTime)).thenReturn(Optional.of(quote));
        when(mapper.toMoney(quote)).thenReturn(expectedPrice);

        // When
        Optional<MarketAssetQuote> result = service.getHistoricalQuote(testIdentifier, dateTime);

        // Then
        assertThat(result.get().currentPrice()).isEqualTo(expectedPrice);
    }

    @Test
    @DisplayName("Should fetch batch prices successfully")
    void shouldFetchBatchPrices() {
        // Given
        List<AssetIdentifier> symbols = List.of(
                createMarketIdentifier("AAPL", "Apple"),
                createMarketIdentifier("GOOGL", "Google"));

        Map<String, ProviderQuote> providerQuotes = Map.of(
                "AAPL", createQuote("AAPL", "150.00"),
                "GOOGL", createQuote("GOOGL", "140.00"));

        // Implementation calls mapper.toProviderSymbol for the batch list
        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(any(AssetIdentifier.class), eq("YAHOO_FINANCE")))
                .thenAnswer(inv -> inv.getArgument(0, AssetIdentifier.class).getPrimaryId());

        when(provider.fetchBatchQuotes(anyList())).thenReturn(providerQuotes);

        // Mocking individual toMoney calls
        when(mapper.toMoney(any(ProviderQuote.class))).thenAnswer(inv -> {
            ProviderQuote q = inv.getArgument(0);
            return new Money(q.price(), ValidatedCurrency.USD);
        });

        // When
        Map<AssetIdentifier, MarketAssetQuote> result = service.getBatchQuotes(symbols);

        // Then
        assertThat(result).hasSize(2);
        verify(provider).fetchBatchQuotes(argThat(list -> list.contains("AAPL") && list.contains("GOOGL")));
    }

    @Test
    @DisplayName("Should fetch asset info successfully")
    void shouldFetchAssetInfo() {
        // Given
        ProviderAssetInfo providerInfo = mock(ProviderAssetInfo.class);
        MarketAssetInfo expectedInfo = mock(MarketAssetInfo.class);
        when(expectedInfo.getName()).thenReturn("Apple Inc.");

        when(provider.fetchAssetInfo("AAPL")).thenReturn(Optional.of(providerInfo));
        when(mapper.toAssetInfo(providerInfo)).thenReturn(expectedInfo);

        // When
        Optional<MarketAssetInfo> result = service.getAssetInfo(testIdentifier);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Apple Inc.");
    }

    @Test
    @DisplayName("Should check symbol support")
    void shouldCheckSymbolSupport() {
        // Given
        when(provider.supportsSymbol("AAPL")).thenReturn(true);

        // When/Then
        assertThat(service.isSymbolSupported(testIdentifier)).isTrue();
        verify(provider).supportsSymbol("AAPL");
    }

    @Test
    @DisplayName("getAssetInfo(String): Should construct internal identifier and delegate")
    void getAssetInfo_StringSymbol_Success() {
        // Given
        String rawSymbol = "MSFT";
        ProviderAssetInfo providerInfo = mock(ProviderAssetInfo.class);
        MarketAssetInfo expectedInfo = mock(MarketAssetInfo.class);

        // Implementation calls provider.fetchAssetInfo(identifier.getPrimaryId())
        when(provider.fetchAssetInfo(rawSymbol)).thenReturn(Optional.of(providerInfo));
        when(mapper.toAssetInfo(providerInfo)).thenReturn(expectedInfo);

        // When
        Optional<MarketAssetInfo> result = service.getAssetInfo(SymbolIdentifier.of("MSFT"));

        // Then
        assertThat(result).isPresent().contains(expectedInfo);
        verify(provider).fetchAssetInfo(rawSymbol);
    }

    @Test
    @DisplayName("getBatchAssetInfo: Should map identifiers to provider symbols and back to domain models")
    void getBatchAssetInfo_Success() {
        // Given
        MarketIdentifier msft = createMarketIdentifier("MSFT", "Microsoft");
        MarketIdentifier goog = createMarketIdentifier("GOOGL", "Google");
        List<AssetIdentifier> symbols = List.of(msft, goog);

        ProviderAssetInfo msftProviderInfo = mock(ProviderAssetInfo.class);
        ProviderAssetInfo googProviderInfo = mock(ProviderAssetInfo.class);

        MarketAssetInfo msftDomainInfo = mock(MarketAssetInfo.class);
        MarketAssetInfo googDomainInfo = mock(MarketAssetInfo.class);

        // Implementation logic stubs
        when(provider.getProviderName()).thenReturn("YAHOO_FINANCE");
        when(mapper.toProviderSymbol(any(AssetIdentifier.class), eq("YAHOO_FINANCE")))
                .thenAnswer(inv -> inv.getArgument(0, AssetIdentifier.class).getPrimaryId());

        Map<String, ProviderAssetInfo> providerResult = Map.of(
                "MSFT", msftProviderInfo,
                "GOOGL", googProviderInfo);
        when(provider.fetchBatchAssetInfo(anyList())).thenReturn(providerResult);

        when(mapper.toAssetInfo(msftProviderInfo)).thenReturn(msftDomainInfo);
        when(mapper.toAssetInfo(googProviderInfo)).thenReturn(googDomainInfo);

        // When
        Map<AssetIdentifier, MarketAssetInfo> result = service.getBatchAssetInfo(symbols);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(msft)).isEqualTo(msftDomainInfo);
        assertThat(result.get(goog)).isEqualTo(googDomainInfo);
        verify(provider).fetchBatchAssetInfo(argThat(list -> list.containsAll(List.of("MSFT", "GOOGL"))));
    }

    @Test
    @DisplayName("getTradingCurrency: Should return currency when asset info is found")
    void getTradingCurrency_Success() {
        // Given
        MarketAssetInfo info = mock(MarketAssetInfo.class);
        when(info.getCurrency()).thenReturn(ValidatedCurrency.USD);

        // Logic check: Implementation calls getAssetInfo internally
        when(provider.fetchAssetInfo("AAPL")).thenReturn(Optional.of(mock(ProviderAssetInfo.class)));
        when(mapper.toAssetInfo(any())).thenReturn(info);

        // When
        ValidatedCurrency currency = service.getTradingCurrency(testIdentifier);

        // Then
        assertThat(currency).isEqualTo(ValidatedCurrency.USD);
    }

    @Test
    @DisplayName("getTradingCurrency: Should throw exception when asset info is missing")
    void getTradingCurrency_NotFound_ThrowsException() {
        // Given
        when(provider.fetchAssetInfo("AAPL")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getTradingCurrency(testIdentifier))
                .isInstanceOf(MarketDataException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.SYMBOL_NOT_FOUND);
    }

    @Test
    @DisplayName("getBatchPrices: Should return empty map for null or empty list")
    void getBatchPrices_EmptyInput_ReturnsEmptyMap() {
        // Test Null
        assertThat(service.getBatchQuotes(null)).isEmpty();

        // Test Empty
        assertThat(service.getBatchQuotes(Collections.emptyList())).isEmpty();

        // Verify provider was never touched
        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("getBatchAssetInfo: Should return empty map for null or empty list")
    void getBatchAssetInfo_EmptyInput_ReturnsEmptyMap() {
        assertThat(service.getBatchAssetInfo(null)).isEmpty();
        assertThat(service.getBatchAssetInfo(Collections.emptyList())).isEmpty();

        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("getBatchPrices: Should skip symbols missing from provider response")
    void getBatchPrices_PartialResults_SkipsMissing() {
        // Given: Requested AAPL and MSFT
        MarketIdentifier apple = createMarketIdentifier("AAPL", "Apple");
        MarketIdentifier msft = createMarketIdentifier("MSFT", "Microsoft");
        List<AssetIdentifier> symbols = List.of(apple, msft);

        // Provider only returns AAPL
        Map<String, ProviderQuote> providerQuotes = Map.of(
                "AAPL", createQuote("AAPL", "150.00"));

        when(provider.getProviderName()).thenReturn("TEST");
        when(mapper.toProviderSymbol(any(), anyString()))
                .thenAnswer(i -> ((AssetIdentifier) i.getArgument(0)).getPrimaryId());
        when(provider.fetchBatchQuotes(anyList())).thenReturn(providerQuotes);
        when(mapper.toMoney(any())).thenReturn(new Money(new BigDecimal("150.00"), ValidatedCurrency.USD));

        // When
        Map<AssetIdentifier, MarketAssetQuote> result = service.getBatchQuotes(symbols);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(apple);
        assertThat(result).doesNotContainKey(msft); // MSFT hit the 'else' branch and was logged/skipped
    }

    @Test
    @DisplayName("getAssetInfo: Should return Optional.empty when provider returns nothing")
    void getAssetInfo_NotFound_ReturnsEmpty() {
        // Given
        when(provider.fetchAssetInfo("UNKNOWN")).thenReturn(Optional.empty());

        // When
        Optional<MarketAssetInfo> result = service
                .getAssetInfo(new MarketIdentifier("UNKNOWN", null, AssetType.STOCK, "name", "USD", null));

        // Then
        assertThat(result).isEmpty();
        verify(mapper, never()).toAssetInfo(any()); // Ensure mapping logic was skipped
    }

    @Test
    @DisplayName("getHistoricalPrice: Should throw Exception when provider returns empty")
    void getHistoricalPrice_EmptyResponse_ThrowsException() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        when(provider.fetchHistoricalQuote("AAPL", now)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getHistoricalQuote(testIdentifier, now))
                .isInstanceOf(MarketDataException.class)
                // Verify it uses the DATA_UNAVAILABLE type specifically
                .extracting("errorType")
                .isEqualTo(ErrorType.DATA_UNAVAILABLE);

        verify(mapper, never()).toMoney(any());
    }

    @Test
    @DisplayName("getBatchAssetInfo: Should log warning and skip symbol when provider info is null")
    void getBatchAssetInfo_PartialResults_SkipsNullInfo() {
        // Given: Requesting two symbols
        MarketIdentifier apple = createMarketIdentifier("AAPL", "Apple");
        MarketIdentifier msft = createMarketIdentifier("MSFT", "Microsoft");
        List<AssetIdentifier> symbols = List.of(apple, msft);

        // Provider only returns data for AAPL
        Map<String, ProviderAssetInfo> partialMap = new HashMap<>();
        partialMap.put("AAPL", mock(ProviderAssetInfo.class));
        // MSFT is explicitly missing from this map

        when(provider.getProviderName()).thenReturn("YAHOO");
        when(mapper.toProviderSymbol(any(), anyString()))
                .thenAnswer(inv -> ((AssetIdentifier) inv.getArgument(0)).getPrimaryId());

        when(provider.fetchBatchAssetInfo(anyList())).thenReturn(partialMap);
        when(mapper.toAssetInfo(any())).thenReturn(mock(MarketAssetInfo.class));

        // When
        Map<AssetIdentifier, MarketAssetInfo> result = service.getBatchAssetInfo(symbols);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(apple);
        assertThat(result).doesNotContainKey(msft);

        // Verify mapping only happened once for the valid entry
        verify(mapper, times(1)).toAssetInfo(any());
    }

    @Test
    @DisplayName("getBatchAssetInfo: Should return empty map immediately if input is null or empty")
    void getBatchAssetInfo_NullOrEmptyInput_ReturnsEmptyMap() {
        // Null check
        assertThat(service.getBatchAssetInfo(null)).isEmpty();

        // Empty list check
        assertThat(service.getBatchAssetInfo(Collections.emptyList())).isEmpty();

        // Verify execution stopped before calling mapper or provider
        verifyNoInteractions(provider);
        verifyNoInteractions(mapper);
    }
    
    // --- Helpers ---

    private MarketIdentifier createMarketIdentifier(String symbol, String name) {
        return new MarketIdentifier(symbol, null, AssetType.STOCK, name, "USD", null);
    }

    private ProviderQuote createQuote(String symbol, String price) {
        return new ProviderQuote(
                symbol,
                new BigDecimal(price),
                "USD",
                LocalDateTime.now(),
                null, null, "YAHOO");
    }
}