package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.BankOfCanadaConfigurationProperties;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos.BocExchangeRateResponse;

@ExtendWith(MockitoExtension.class)
class BocApiClientTest {

    @Mock
    private BankOfCanadaConfigurationProperties config;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<Object> httpResponse;

    private BocApiClient client;

    @BeforeEach
    void setup() {
        when(config.getBaseUrl()).thenReturn("https://api.test");
        when(config.getTimeoutSeconds()).thenReturn(5);
        lenient().when(config.isDebugLogging()).thenReturn(true);
        client = new BocApiClient(config, objectMapper, httpClient);
    }

    @Test
    void getLatestExchangeRate_success() throws Exception {
        String json = "{ \"observations\": [] }";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);
        lenient().when(config.isDebugLogging()).thenReturn(false);

        BocExchangeRateResponse expected = new BocExchangeRateResponse();
        when(objectMapper.readValue(json, BocExchangeRateResponse.class))
                .thenReturn(expected);

        BocExchangeRateResponse result = client.getLatestExchangeRate("USD", "CAD");

        assertSame(expected, result);
    }

    @Test
    void getLatestExchangeRate_jsonParseFailure() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("bad-json");
        when(httpClient.send(any(), any())).thenReturn(httpResponse);


        when(objectMapper.readValue(anyString(), eq(BocExchangeRateResponse.class)))
                .thenThrow(new BocApiException("boom"));

        assertThrows(BocApiException.class,
                () -> client.getLatestExchangeRate("USD", "CAD"));
    }

    @Test
    void getHistoricalExchangeRate_success() throws Exception {
        String json = "{ \"observations\": [] }";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);
        lenient().when(config.isDebugLogging()).thenReturn(false);

        BocExchangeRateResponse response = new BocExchangeRateResponse();
        when(objectMapper.readValue(json, BocExchangeRateResponse.class))
                .thenReturn(response);

        BocExchangeRateResponse result = client.getHistoricalExchangeRate(
                "USD",
                "CAD",
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now());

        assertSame(response, result);
    }

    @Test
    void getHistoricalExchangeRate_networkFailure() throws Exception {
        when(httpClient.send(any(), any()))
                .thenThrow(new IOException("network down"));

        BocApiException ex = assertThrows(
                BocApiException.class,
                () -> client.getHistoricalExchangeRate(
                        "USD",
                        "CAD",
                        LocalDateTime.now().minusDays(5),
                        LocalDateTime.now()));

        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    void executeGetRequest_400() throws Exception {
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        BocApiException ex = assertThrows(
                BocApiException.class,
                () -> client.getLatestExchangeRate("USD", "CAD"));

        assertTrue(ex.getMessage()
                .contains("Failed to fetch latest exchange rate"));

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage()
                .contains("Invalid inputs"));
    }

    @Test
    void executeGetRequest_404() throws Exception {
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);
        lenient().when(config.isDebugLogging()).thenReturn(false);

        assertThrows(BocApiException.class,
                () -> client.getLatestExchangeRate("USD", "CAD"));
    }

    @Test
    void executeGetRequest_418() throws Exception {
        when(httpResponse.statusCode()).thenReturn(418);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        BocApiException ex = assertThrows(BocApiException.class,
                () -> client.getLatestExchangeRate("USD", "CAD"));

        assertTrue(ex.getCause().getMessage()
                .contains("FMP API error"));
    }

    @Test
    void executeGetRequest_500() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        assertThrows(BocApiException.class,
                () -> client.getLatestExchangeRate("USD", "CAD"));
    }

    @Test
    void executeGetRequest_ioException() throws Exception {
        when(httpClient.send(any(), any()))
                .thenThrow(new IOException("network down"));

        assertThrows(BocApiException.class,
                () -> client.getLatestExchangeRate("USD", "CAD"));
    }

    @Test
    void executeGetRequest_interruptedException() throws Exception {
        when(httpClient.send(any(), any()))
                .thenThrow(new InterruptedException("interrupted"));

        assertThrows(BocApiException.class,
                () -> client.getLatestExchangeRate("USD", "CAD"));

        assertTrue(Thread.currentThread().isInterrupted());
    }
}
