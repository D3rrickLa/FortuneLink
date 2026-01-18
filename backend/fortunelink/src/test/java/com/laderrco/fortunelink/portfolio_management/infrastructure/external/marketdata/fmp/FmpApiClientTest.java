package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.FmpConfigurationProperties;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.FmpApiException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.dtos.FmpQuoteResponse;

@ExtendWith(MockitoExtension.class)
class FmpApiClientTest {

    @Mock
    private FmpConfigurationProperties config;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private FmpApiClient client;
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "https://api.fmp.com"; // i know it's not but too lazy to change it
    private static final String API_KEY = "test-key";

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();

        when(config.getBaseUrl()).thenReturn(BASE_URL);
        when(config.getApiKey()).thenReturn(API_KEY);
        when(config.getTimeoutSeconds()).thenReturn(5);
        lenient().when(config.isDebugLogging()).thenReturn(false);
        doNothing().when(config).validate();

        client = new FmpApiClient(config, objectMapper, httpClient);
    }

    // -------------------------------------------------------
    // Helper to load JSON fixtures
    // -------------------------------------------------------

    private String loadJson(String path) throws Exception {
        return new String(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(path)).readAllBytes(),
                StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------
    // QUOTES
    // -------------------------------------------------------

    @Test
    void getQuote_usStock_and_internationalEtf_success() throws Exception {
        // AAPL
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body())
                .thenReturn(loadJson("fmp/quote-aapl.json"))
                .thenReturn(loadJson("fmp/quote-vfv.json"));

        FmpQuoteResponse aapl = client.getQuote("AAPL");
        FmpQuoteResponse vfv = client.getQuote("VFV.TO");

        // US equity assertions
        assertEquals("AAPL", aapl.getSymbol());
        assertEquals("NASDAQ", aapl.getExchange());
        assertTrue(aapl.getPrice().compareTo(BigDecimal.ZERO) > 0);

        // International ETF assertions
        assertEquals("VFV.TO", vfv.getSymbol());
        assertEquals("TSX", vfv.getExchange());
        assertTrue(vfv.getPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    // -------------------------------------------------------
    // PROFILES
    // -------------------------------------------------------

    @Test
    void getProfile_stock_vs_etf_success() throws Exception {
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body())
                .thenReturn(loadJson("fmp/profile-aapl.json"))
                .thenReturn(loadJson("fmp/profile-vfv.json"));

        FmpProfileResponse aapl = client.getProfile("AAPL");
        FmpProfileResponse vfv = client.getProfile("VFV.TO");

        // Stock profile
        assertEquals("Apple Inc.", aapl.getCompanyName());
        assertFalse(aapl.getIsEtf());
        assertEquals("Technology", aapl.getSector());
        assertEquals("US", aapl.getCountry());

        // ETF profile
        assertTrue(vfv.getIsEtf());
        assertEquals("CAD", vfv.getCurrency());
        assertEquals("TSX", vfv.getExchange());
        assertEquals("Financial Services", vfv.getSector());
    }

    // -------------------------------------------------------
    // BATCH QUOTES (FREE TIER SEQUENTIAL)
    // -------------------------------------------------------

    @Test
    void getBatchQuotes_filtersFailures() throws Exception {
        // First call succeeds, second call fails at HTTP level
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse)
                .thenThrow(new IOException("Simulated failure"));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body())
                .thenReturn(loadJson("fmp/quote-aapl.json"));

        List<FmpQuoteResponse> results = client.getBatchQuotes(List.of("AAPL", "FAIL"));

        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getSymbol());
    }

    // -------------------------------------------------------
    // ERROR HANDLING
    // -------------------------------------------------------

    @Test
    void getQuote_emptyPayload_throwsException() throws Exception {
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("[]");

        assertThrows(FmpApiException.class,
                () -> client.getQuote("UNKNOWN"));
    }

    @Test
    void getQuote_unauthorized_throwsException() throws Exception {
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(401);

        assertThrows(FmpApiException.class,
                () -> client.getQuote("AAPL"));
    }
}