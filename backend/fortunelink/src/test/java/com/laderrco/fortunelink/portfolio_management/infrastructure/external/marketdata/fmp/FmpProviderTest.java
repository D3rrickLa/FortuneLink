package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpQuoteResponse;

@ExtendWith(MockitoExtension.class)
class FmpProviderTest {

    @Mock
    private FmpApiClient fmpApiClient;

    @Mock
    private FmpResponseMapper mapper;

    @InjectMocks
    private FmpProvider fmpProvider;

    @Test
    @DisplayName("fetchCurrentQuote: Should return empty Optional on API exception")
    void fetchCurrentQuote_Exception_ReturnsEmpty() {
        when(fmpApiClient.getQuote("BAD")).thenThrow(new RuntimeException("API Down"));

        Optional<ProviderQuote> result = fmpProvider.fetchCurrentQuote("BAD");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchBatchQuotes: Should handle successful batch return")
    void fetchBatchQuotes_Success() {
        FmpQuoteResponse res = mock(FmpQuoteResponse.class);
        when(res.getSymbol()).thenReturn("AAPL");
        when(fmpApiClient.getBatchQuotes(anyList())).thenReturn(List.of(res));
        when(mapper.toProviderQuote(any())).thenReturn(mock(ProviderQuote.class));

        Map<String, ProviderQuote> result = fmpProvider.fetchBatchQuotes(List.of("AAPL"));

        assertThat(result).containsKey("AAPL");
        verify(fmpApiClient).getBatchQuotes(anyList());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("fetchBAstAssetInfo should reutrn empty when symbol is null")
    void fetchBatchQoutes_ReturnsEmpty_WhenSymbolsNull(List<String> symbols) {
        Map<String, ProviderQuote> map = fmpProvider.fetchBatchQuotes(symbols);
        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("fetchBatchAssetInfo: Should continue if one profile fails")
    void fetchBatchAssetInfo_PartialFailure_ReturnsAvailable() {
        // Given: AAPL succeeds, GOOG fails
        FmpProfileResponse appleRes = mock(FmpProfileResponse.class);
        when(appleRes.getSymbol()).thenReturn("AAPL");

        when(fmpApiClient.getProfile("AAPL")).thenReturn(appleRes);
        when(fmpApiClient.getProfile("GOOG")).thenThrow(new RuntimeException("404"));
        when(mapper.toProviderAssetInfo(appleRes)).thenReturn(mock(ProviderAssetInfo.class));

        // When
        Map<String, ProviderAssetInfo> result = fmpProvider.fetchBatchAssetInfo(List.of("AAPL", "GOOG"));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey("AAPL");
        assertThat(result).doesNotContainKey("GOOG");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("fetchBAstAssetInfo should reutrn empty when symbol is null")
    void fetchBatchAssetInfo_ReturnsEmpty_WhenSymbolsNull(List<String> symbols) {
        Map<String, ProviderAssetInfo> map = fmpProvider.fetchBatchAssetInfo(symbols);
        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("supportsSymbol: Should correctly validate symbol patterns")
    void supportsSymbol_ValidatesCorrectly() {
        assertThat(fmpProvider.supportsSymbol("AAPL")).isTrue();
        assertThat(fmpProvider.supportsSymbol("BRK.B")).isTrue();
        assertThat(fmpProvider.supportsSymbol("BTC-USD")).isTrue();
        assertThat(fmpProvider.supportsSymbol("^GSPC")).isTrue();

        assertThat(fmpProvider.supportsSymbol("aapl")).isFalse(); // FMP usually expects uppercase
        assertThat(fmpProvider.supportsSymbol(" ")).isFalse();
        assertThat(fmpProvider.supportsSymbol(null)).isFalse();
    }

    @Test
    @DisplayName("fetchCurrentQuote: Successful path")
    void fetchCurrentQuote_Success() {
        FmpQuoteResponse mockRes = mock(FmpQuoteResponse.class);
        ProviderQuote mockQuote = mock(ProviderQuote.class);

        when(fmpApiClient.getQuote("AAPL")).thenReturn(mockRes);
        when(mapper.toProviderQuote(mockRes)).thenReturn(mockQuote);

        Optional<ProviderQuote> result = fmpProvider.fetchCurrentQuote("AAPL");

        assertThat(result).isPresent().contains(mockQuote);
    }

    @Test
    @DisplayName("fetchCurrentQuote: Catch block coverage")
    void fetchCurrentQuote_CatchBlock() {
        when(fmpApiClient.getQuote(anyString())).thenThrow(new RuntimeException("Network Error"));

        Optional<ProviderQuote> result = fmpProvider.fetchCurrentQuote("AAPL");

        assertThat(result).isEmpty();
        // Log is implicitly tested via the behavior
    }

    @Test
    @DisplayName("fetchHistoricalQuote: Should throw UnsupportedOperationException")
    void fetchHistoricalQuote_ThrowsException() {
        assertThatThrownBy(() -> fmpProvider.fetchHistoricalQuote("AAPL", LocalDateTime.now()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unimplemented method 'fetchHistoricalQuote'");
    }

    @Test
    @DisplayName("fetchBatchQuotes: Catch block coverage")
    void fetchBatchQuotes_CatchBlock() {
        when(fmpApiClient.getBatchQuotes(anyList())).thenThrow(new RuntimeException("Batch Failed"));

        Map<String, ProviderQuote> result = fmpProvider.fetchBatchQuotes(List.of("AAPL", "MSFT"));

        assertThat(result).isEmpty();
        assertThat(fmpProvider.getProviderName()).isEqualTo("FMP");
    }

    @Test
    @DisplayName("fetchAssetInfo: Successful path")
    void fetchAssetInfo_Success() {
        FmpProfileResponse mockRes = mock(FmpProfileResponse.class);
        ProviderAssetInfo mockInfo = mock(ProviderAssetInfo.class);

        when(fmpApiClient.getProfile("AAPL")).thenReturn(mockRes);
        when(mapper.toProviderAssetInfo(mockRes)).thenReturn(mockInfo);

        Optional<ProviderAssetInfo> result = fmpProvider.fetchAssetInfo("AAPL");

        assertThat(result).isPresent().contains(mockInfo);
    }

    @Test
    @DisplayName("fetchAssetInfo: Catch block coverage")
    void fetchAssetInfo_CatchBlock() {
        when(fmpApiClient.getProfile(anyString())).thenThrow(new RuntimeException("Profile Error"));

        Optional<ProviderAssetInfo> result = fmpProvider.fetchAssetInfo("AAPL");

        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Testing the fetchBatchQuotes lambda 3")
    void fetchBatchQuotes_mapsResponsesCorrectly() {
        // Arrange: create a sample FmpQuoteResponse
        FmpQuoteResponse response = new FmpQuoteResponse();
        response.setSymbol("AAPL"); // Must set symbol for map key

        // Mock the API client to return a list with the response
        when(fmpApiClient.getBatchQuotes(List.of("AAPL"))).thenReturn(List.of(response));

        // Mock the mapper to return a ProviderQuote for the response
        ProviderQuote providerQuote = new ProviderQuote("AAPL", BigDecimal.TEN, "USD", LocalDateTime.now(), "FMP");
        when(mapper.toProviderQuote(response)).thenReturn(providerQuote);

        // Act: call the method under test
        Map<String, ProviderQuote> result = fmpProvider.fetchBatchQuotes(List.of("AAPL"));

        // Assert: check that the map contains the expected entry
        assertEquals(1, result.size());
        assertSame(providerQuote, result.get("AAPL"));

        // Verify: ensure the mapper lambda was actually called
        verify(mapper).toProviderQuote(response);
    }

    @Test
    void fetchBatchQuotes_shouldHandleDuplicateSymbols() {
        // Arrange: 3 symbols, but only 2 are unique
        List<String> symbols = List.of("AAPL", "AAPL", "MSFT");

        FmpQuoteResponse quote1 = new FmpQuoteResponse();
        quote1.setSymbol("AAPL");
        quote1.setPrice(BigDecimal.valueOf(150.0));

        // CHANGE: This should be MSFT so the Map has two distinct keys
        FmpQuoteResponse quote2 = new FmpQuoteResponse();
        quote2.setSymbol("MSFT");
        quote2.setPrice(BigDecimal.valueOf(200.0));

        ProviderQuote mockProviderQuote = mock(ProviderQuote.class);

        // Mock the API to return the two unique responses found by the API
        when(fmpApiClient.getBatchQuotes(any())).thenReturn(List.of(quote1, quote2));

        // Mock the mapper
        when(mapper.toProviderQuote(any(FmpQuoteResponse.class))).thenReturn(mockProviderQuote);

        // Act
        Map<String, ProviderQuote> result = fmpProvider.fetchBatchQuotes(symbols);

        // Assert
        // Result size is 2 because AAPL is deduplicated by the Map keys
        assertEquals(2, result.size(), "Map should have 2 entries (AAPL and MSFT)");
        verify(fmpApiClient, times(1)).getBatchQuotes(any());
    }

    @Test
    void fetchBatchAssetInfo_shouldHandleDuplicateSymbolsInMergeFunction() {
        // 1. Arrange: Input has duplicates
        List<String> symbols = List.of("AAPL", "AAPL");

        FmpProfileResponse profile1 = new FmpProfileResponse();
        profile1.setSymbol("AAPL");
        profile1.setCompanyName("Apple Inc (First)");

        FmpProfileResponse profile2 = new FmpProfileResponse();
        profile2.setSymbol("AAPL");
        profile2.setCompanyName("Apple Inc (Second)");

        ProviderAssetInfo mappedInfo = mock(ProviderAssetInfo.class); // Your domain model

        // 2. Mock: First call returns profile1, second call returns profile2
        when(fmpApiClient.getProfile("AAPL"))
                .thenReturn(profile1)
                .thenReturn(profile2);

        // Mock Mapper
        when(mapper.toProviderAssetInfo(any(FmpProfileResponse.class))).thenReturn(mappedInfo);

        // 3. Act
        Map<String, ProviderAssetInfo> result = fmpProvider.fetchBatchAssetInfo(symbols);

        // 4. Assert
        assertEquals(1, result.size(), "Map should collapse duplicates to a single entry");
        assertTrue(result.containsKey("AAPL"));

        // Verify interactions
        verify(fmpApiClient, times(2)).getProfile("AAPL");
    }
}