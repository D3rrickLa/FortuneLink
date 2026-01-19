package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
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

        lenient().when(config.getBaseUrl()).thenReturn(BASE_URL);
        lenient().when(config.getApiKey()).thenReturn(API_KEY);
        lenient().when(config.getTimeoutSeconds()).thenReturn(5);
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
        when(config.isDebugLogging()).thenReturn(true);
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

    @SuppressWarnings("unchecked")
    @Test
    void getQuote_SuccessfulMapping() throws Exception {
        // GIVEN: The exact JSON from your log
        String json = """
                [
                  {
                    "symbol": "AAPL",
                    "name": "Apple Inc.",
                    "price": 255.517,
                    "changePercentage": -1.04295,
                    "change": -2.693,
                    "volume": 72142773,
                    "dayLow": 254.93,
                    "dayHigh": 258.9,
                    "yearHigh": 288.62,
                    "yearLow": 169.21,
                    "marketCap": 3775609234912.9995,
                    "priceAvg50": 271.5098,
                    "priceAvg200": 234.05525,
                    "exchange": "NASDAQ",
                    "open": 257.88,
                    "previousClose": 258.21,
                    "timestamp": 1768597201
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200); 
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        // WHEN
        FmpQuoteResponse result = client.getQuote("AAPL");

        // THEN
        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals(BigDecimal.valueOf(255.517), result.getPrice());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getQuote_throwsFmpApiException_whenJacksonFails() throws Exception {

        String mockJson = "[{\"symbol\":\"AAPL\"}]";
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        client = new FmpApiClient(config, objectMapper, httpClient);

        // We need to mock the httpClient.send() to return a 200 response
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockJson);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // 2Force the objectMapper to throw the Checked Exception
        // Note: We use any() or the specific TypeReference constant
        lenient().doThrow(new JsonMappingException(null, "Jackson parse error"))
                .when(objectMapper)
                .readValue(anyString(), any(TypeReference.class));
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new JsonMappingException(null, "Jackson parse error"));
        // JsonMappingException is a subclass of IOException

        FmpApiException exception = assertThrows(FmpApiException.class, () -> {
            client.getQuote("AAPL");
        });

        assertTrue(exception.getMessage().contains("Failed to parse FMP quote response"));
        assertEquals(JsonMappingException.class, exception.getCause().getClass());
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

    @Test
    @SuppressWarnings("unchecked")
    void getProfile_throwsFmpApiException_whenJacksonFails() throws Exception {

        String mockJson = "[{\"symbol\":\"AAPL\"}]";
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        client = new FmpApiClient(config, objectMapper, httpClient);

        // We need to mock the httpClient.send() to return a 200 response
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockJson);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // 2Force the objectMapper to throw the Checked Exception
        // Note: We use any() or the specific TypeReference constant
        lenient().doThrow(new JsonMappingException(null, "Jackson parse error"))
                .when(objectMapper)
                .readValue(anyString(), any(TypeReference.class));
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new JsonMappingException(null, "Jackson parse error"));
        // JsonMappingException is a subclass of IOException

        FmpApiException exception = assertThrows(FmpApiException.class, () -> {
            client.getProfile("AAPL");
        });

        assertTrue(exception.getMessage().contains("Failed to parse FMP profile response"));
        assertEquals(JsonMappingException.class, exception.getCause().getClass());
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

    @ParameterizedTest
    @NullAndEmptySource
    void getBatchQuotes_ReturnEmptyWhenSymbolsNotGiven(List<String> symbols) {

        List<FmpQuoteResponse> results = client.getBatchQuotes(symbols);

        assertEquals(0, results.size());
    }

    // -------------------------------------------------------
    // BATCH PROFILES (FREE TIER SEQUENTIAL)
    // -------------------------------------------------------

    @Test
    void getBatchProfiles_filtersFailures() throws Exception {
        // First call succeeds, second call fails at HTTP level
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse)
                .thenThrow(new IOException("Simulated failure"));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body())
                .thenReturn(loadJson("fmp/profile-aapl.json"));

        List<FmpProfileResponse> results = client.getBatchProfiles(List.of("AAPL", "FAIL"));

        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getSymbol());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getBatchProfiles_ReturnEmptyWhenSymbolsNotGiven(List<String> symbols) {

        List<FmpProfileResponse> results = client.getBatchProfiles(symbols);

        assertEquals(0, results.size());
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
    void getProfile_emptyPayload_throwsException() throws Exception {
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("[]");

        assertThrows(FmpApiException.class,
                () -> client.getProfile("UNKNOWN"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 401, 429, 404, 503 })
    void getQuote_unauthorized_throwsException(int error) throws Exception {
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(error);

        assertThrows(FmpApiException.class,
                () -> client.getQuote("AAPL"));
    }

    @Test
    void testIfValidateConfig() {
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void getProfile_throwsInterruptedException() throws IOException, InterruptedException {
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(InterruptedException.class);

        assertThrows(FmpApiException.class, () -> client.getProfile("AAPL"));

    }
}