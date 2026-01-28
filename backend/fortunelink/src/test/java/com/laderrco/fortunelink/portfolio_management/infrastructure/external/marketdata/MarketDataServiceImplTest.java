package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataProvider;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
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
        testIdentifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);

        // Common stub for the provider name used in many service methods
        lenient().when(provider.getProviderName()).thenReturn("YAHOO");
    }

    @Test
    @DisplayName("Should fetch current quote successfully")
    void shouldFetchCurrentQuote() {
        // Given
        ProviderQuote pQuote = createQuote("AAPL", "150.00");
        MarketAssetQuote mQuote = mock(MarketAssetQuote.class);

        // Implementation logic: 1. map to provider symbol, 2. fetch, 3. map to domain
        // quote
        when(mapper.toProviderSymbol(testIdentifier, "YAHOO")).thenReturn("AAPL");
        when(provider.fetchCurrentQuote("AAPL")).thenReturn(Optional.of(pQuote));
        when(mapper.toAssetQuote(testIdentifier, pQuote)).thenReturn(mQuote);

        // When
        Optional<MarketAssetQuote> result = service.getCurrentQuote(testIdentifier);

        // Then
        assertThat(result).isPresent().contains(mQuote);
        verify(provider).fetchCurrentQuote("AAPL");
    }

    @Test
    @DisplayName("getCurrentQuote: Should return empty Optional when provider returns no data")
    void getCurrentQuote_ProviderReturnsEmpty_ReturnsEmptyOptional() {
        // 1. Arrange
        // We need to satisfy the mapper.toProviderSymbol call first
        String providerName = "YAHOO";
        String symbol = "AAPL";
        when(provider.getProviderName()).thenReturn(providerName);
        when(mapper.toProviderSymbol(testIdentifier, providerName)).thenReturn(symbol);

        // This is the core of the test: the provider finds nothing
        when(provider.fetchCurrentQuote(symbol)).thenReturn(Optional.empty());

        // 2. Act
        Optional<MarketAssetQuote> result = service.getCurrentQuote(testIdentifier);

        // 3. Assert
        assertThat(result).isEmpty();

        // Verification: Ensure we never tried to map a null quote
        verify(provider).fetchCurrentQuote(symbol);
        verify(mapper, never()).toAssetQuote(any(), any());
    }

    @Test
    @DisplayName("Should fetch historical quote successfully")
    void shouldFetchHistoricalQuote() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 0);
        ProviderQuote pQuote = createQuote("AAPL", "145.00");
        MarketAssetQuote mQuote = mock(MarketAssetQuote.class);

        when(mapper.toProviderSymbol(testIdentifier, "YAHOO")).thenReturn("AAPL");
        when(provider.fetchHistoricalQuote("AAPL", dateTime)).thenReturn(Optional.of(pQuote));
        when(mapper.toAssetQuote(testIdentifier, pQuote)).thenReturn(mQuote);

        // When
        Optional<MarketAssetQuote> result = service.getHistoricalQuote(testIdentifier, dateTime);

        // Then
        assertThat(result).isPresent().contains(mQuote);
        verify(provider).fetchHistoricalQuote("AAPL", dateTime);
    }

    @Test
    @DisplayName("getHistoricalQuote: Should return empty Optional when provider returns no historical data")
    void getHistoricalQuote_ProviderReturnsEmpty_ReturnsEmptyOptional() {
        // 1. Arrange
        String providerName = "YAHOO";
        String symbol = "AAPL";
        LocalDateTime testDate = LocalDateTime.of(2023, 10, 1, 12, 0);

        when(provider.getProviderName()).thenReturn(providerName);
        when(mapper.toProviderSymbol(testIdentifier, providerName)).thenReturn(symbol);

        // Mock the historical call specifically
        when(provider.fetchHistoricalQuote(symbol, testDate)).thenReturn(Optional.empty());

        // 2. Act
        Optional<MarketAssetQuote> result = service.getHistoricalQuote(testIdentifier, testDate);

        // 3. Assert
        assertThat(result).isEmpty();

        // Verification
        // FIX: Verify fetchHistoricalQuote, NOT fetchCurrentQuote
        verify(provider).fetchHistoricalQuote(symbol, testDate);

        // Ensure we never called the mapper since the quote was empty
        verify(mapper, never()).toAssetQuote(any(), any());
    }

    @Test
    @DisplayName("Should fetch batch quotes successfully")
    void shouldFetchBatchQuotes() {
        // Given
        List<MarketIdentifier> symbols = List.of(
                createMarketIdentifier("AAPL", "Apple"),
                createMarketIdentifier("GOOGL", "Google"));

        ProviderQuote aaplP = createQuote("AAPL", "150.00");
        ProviderQuote googP = createQuote("GOOGL", "2800.00");

        Map<String, ProviderQuote> providerQuotes = Map.of("AAPL", aaplP, "GOOGL", googP);

        when(mapper.toProviderSymbol(any(), eq("YAHOO")))
                .thenAnswer(inv -> ((AssetIdentifier) inv.getArgument(0)).getPrimaryId());
        when(provider.fetchBatchQuotes(anyList())).thenReturn(providerQuotes);

        // Mocking the mapping for each returned quote
        when(mapper.toAssetQuote(any(), any())).thenReturn(mock(MarketAssetQuote.class));

        // When
        Map<AssetIdentifier, MarketAssetQuote> result = service.getBatchQuotes(symbols);

        // Then
        assertThat(result).hasSize(2);
        verify(provider).fetchBatchQuotes(argThat(list -> list.containsAll(List.of("AAPL", "GOOGL"))));
    }

    @Test
    @DisplayName("getBatchQuotes: Should return empty map immediately if input list is null or empty")
    void getBatchQuotes_NullOrEmptyInput_ReturnsEmptyMap() {
        // Test for null input
        assertThat(service.getBatchQuotes(null)).isEmpty();

        // Test for empty list input
        assertThat(service.getBatchQuotes(Collections.emptyList())).isEmpty();

        // Verify the service didn't even talk to the provider or mapper
        verifyNoInteractions(provider, mapper);
    }

    @Test
    @DisplayName("getBatchQuotes: Should handle unknown provider symbols and mapping exceptions")
    void getBatchQuotes_HandlesNullIdentifierAndMappingExceptions() {
        // --- 1. Arrange ---
        MarketIdentifier apple = createMarketIdentifier("AAPL", "Apple");
        MarketIdentifier msft = createMarketIdentifier("MSFT", "Microsoft");
        List<AssetIdentifier> symbols = List.of(apple, msft);

        // Provider returns 3 quotes:
        // - AAPL (Valid)
        // - MSFT (Will throw exception during mapping)
        // - UNKNOWN (Not in our original request)
        ProviderQuote appleP = createQuote("AAPL", "150.00");
        ProviderQuote msftP = createQuote("MSFT", "250.00");
        ProviderQuote unknownP = createQuote("UNKNOWN", "99.00");

        Map<String, ProviderQuote> providerResponse = new HashMap<>();
        providerResponse.put("AAPL", appleP);
        providerResponse.put("MSFT", msftP);
        providerResponse.put("UNKNOWN", unknownP);

        // Setup basic mapping
        when(provider.getProviderName()).thenReturn("YAHOO");
        when(mapper.toProviderSymbol(apple, "YAHOO")).thenReturn("AAPL");
        when(mapper.toProviderSymbol(msft, "YAHOO")).thenReturn("MSFT");

        when(provider.fetchBatchQuotes(anyList())).thenReturn(providerResponse);

        // Mock behavior for the loop:
        // Success for Apple
        MarketAssetQuote appleM = mock(MarketAssetQuote.class);
        when(mapper.toAssetQuote(apple, appleP)).thenReturn(appleM);

        // Force Exception for Microsoft to trigger the 'catch' block
        when(mapper.toAssetQuote(msft, msftP)).thenThrow(new RuntimeException("Mapping failed"));

        // --- 2. Act ---
        Map<AssetIdentifier, MarketAssetQuote> result = service.getBatchQuotes(symbols);

        // --- 3. Assert ---
        // Should only contain Apple.
        // MSFT failed mapping (caught), UNKNOWN was skipped (id == null)
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(apple);
        assertThat(result).doesNotContainKey(msft);

        // Verify logic gates
        verify(mapper).toAssetQuote(apple, appleP);
        verify(mapper).toAssetQuote(msft, msftP);

        // Check that we never attempted to map the "UNKNOWN" key
        // This proves the 'if (id == null) continue;' worked
        verify(mapper, never()).toAssetQuote(eq(null), any());
    }

    @Test
    @DisplayName("getBatchAssetInfo: Should skip entries where provider symbol does not match requested identifiers")
    void getBatchAssetInfo_ProviderReturnsExtraSymbol_SkipsUnexpectedEntry() {
        // --- 1. Arrange ---
        MarketIdentifier requestedId = createMarketIdentifier("AAPL", "Apple");
        List<AssetIdentifier> requestedList = List.of(requestedId);

        // Mocks for the first loop (Symbol Mapping)
        when(provider.getProviderName()).thenReturn("YAHOO");
        when(mapper.toProviderSymbol(requestedId, "YAHOO")).thenReturn("AAPL_PROVIDER");

        // Mock for the Provider call: returns the requested one AND an unexpected one
        ProviderAssetInfo validInfo = mock(ProviderAssetInfo.class);
        ProviderAssetInfo unexpectedInfo = mock(ProviderAssetInfo.class);

        Map<String, ProviderAssetInfo> providerResponse = new HashMap<>();
        providerResponse.put("AAPL_PROVIDER", validInfo);
        providerResponse.put("SURPRISE_STOCK", unexpectedInfo); // This triggers the id == null check

        when(provider.fetchBatchAssetInfo(anyList())).thenReturn(providerResponse);

        // Mock the successful mapping
        MarketAssetInfo domainInfo = mock(MarketAssetInfo.class);
        when(mapper.toAssetInfo(validInfo)).thenReturn(domainInfo);

        // --- 2. Act ---
        Map<AssetIdentifier, MarketAssetInfo> result = service.getBatchAssetInfo(requestedList);

        // --- 3. Assert ---
        // Should only contain the symbol we actually asked for
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(requestedId);
        assertThat(result.get(requestedId)).isEqualTo(domainInfo);

        // Verification: Ensure the mapper was NEVER called for the unexpected symbol
        // This confirms the 'continue' logic worked
        verify(mapper, times(1)).toAssetInfo(validInfo);
        verify(mapper, never()).toAssetInfo(unexpectedInfo);

        // Explicitly verify no mapping attempt with a null key
        verify(mapper, never()).toAssetInfo(null);
    }

    @Test
    @DisplayName("getTradingCurrency: Should return currency when asset info is found")
    void getTradingCurrency_Success() {
        // Given
        ProviderAssetInfo pInfo = mock(ProviderAssetInfo.class);
        MarketAssetInfo mInfo = mock(MarketAssetInfo.class);

        when(mapper.toProviderSymbol(testIdentifier, "YAHOO")).thenReturn("AAPL");
        when(provider.fetchAssetInfo("AAPL")).thenReturn(Optional.of(pInfo));
        when(mapper.toAssetInfo(pInfo)).thenReturn(mInfo);
        when(mInfo.getCurrency()).thenReturn(ValidatedCurrency.USD);

        // When
        ValidatedCurrency currency = service.getTradingCurrency(testIdentifier);

        // Then
        assertThat(currency).isEqualTo(ValidatedCurrency.USD);
    }

    @Test
    @DisplayName("getTradingCurrency: Should throw exception with SYMBOL_NOT_FOUND when info missing")
    void getTradingCurrency_NotFound_ThrowsException() {
        // Given
        when(mapper.toProviderSymbol(testIdentifier, "YAHOO")).thenReturn("AAPL");
        when(provider.fetchAssetInfo("AAPL")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getTradingCurrency(testIdentifier))
                .isInstanceOf(MarketDataException.class)
                .hasMessageContaining("AAPL")
                .satisfies(ex -> {
                    MarketDataException mde = (MarketDataException) ex;
                    assertThat(mde.getErrorType()).isEqualTo(ErrorType.SYMBOL_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("isSymbolSupported: Should delegate to provider with mapped symbol")
    void shouldCheckSymbolSupport() {
        // Given
        when(mapper.toProviderSymbol(testIdentifier, "YAHOO")).thenReturn("AAPL-PROV");
        when(provider.supportsSymbol("AAPL-PROV")).thenReturn(true);

        // When
        boolean result = service.isSymbolSupported(testIdentifier);

        // Then
        assertThat(result).isTrue();
        verify(provider).supportsSymbol("AAPL-PROV");
    }

    @Test
    @DisplayName("getCurrentPrice: Should return price when quote exists")
    void getCurrentPrice_Success() {
        // 1. Mock the INTERFACE, not the Implementation
        MarketDataService mockService = mock(MarketDataService.class, withSettings()
                .defaultAnswer(CALLS_REAL_METHODS));

        MarketIdentifier testId = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
        Money expectedPrice = Money.of(new BigDecimal("150.00"), ValidatedCurrency.USD);

        // 2. Create a dummy quote
        MarketAssetQuote mockQuote = mock(MarketAssetQuote.class);
        when(mockQuote.currentPrice()).thenReturn(expectedPrice);

        // 3. Stub the abstract method
        when(mockService.getCurrentQuote(testId)).thenReturn(Optional.of(mockQuote));

        // 4. Act on the default method
        Money result = mockService.getCurrentPrice(testId);

        // 5. Assert
        assertEquals(expectedPrice, result);
    }

    @Test
    @DisplayName("getCurrentPrice: Should throw MarketDataException when quote is missing")
    void getCurrentPrice_NotFound_ThrowsException() {
        // Arrange
        MarketIdentifier testId = new MarketIdentifier(
                "AAPL", null, AssetType.STOCK, "Apple", "USD", null);
        when(service.getCurrentQuote(testId)).thenReturn(Optional.empty());

        // Act & Assert
        MarketDataException exception = assertThrows(MarketDataException.class, () -> {
            service.getCurrentPrice(testId);
        });

        assertTrue(exception.getMessage().contains("Price unavailable"));
        assertEquals(ErrorType.DATA_UNAVAILABLE, exception.getErrorType());
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