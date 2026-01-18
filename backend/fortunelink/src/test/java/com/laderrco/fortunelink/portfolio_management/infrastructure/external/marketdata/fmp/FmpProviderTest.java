package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
}