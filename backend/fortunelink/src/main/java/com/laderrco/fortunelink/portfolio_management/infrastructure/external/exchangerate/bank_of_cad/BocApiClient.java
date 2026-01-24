package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.BankOfCanadaConfigurationProperties;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos.BocExchangeRateResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BocApiClient {

    private final BankOfCanadaConfigurationProperties config;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public BocApiClient(BankOfCanadaConfigurationProperties config,
            @Qualifier("defaultObjectMapper") ObjectMapper objectMapper,
            @Qualifier("bocHttpClient") HttpClient httpClient) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;

        log.info("Bank of Canada API Client initialized with base URL: {}", config.getBaseUrl());
        config.validate();
    }

    public BocExchangeRateResponse getLatestExchangeRate(String to, String from) {
        List<String> series = BocCurrencyPairResolver.resolveSeries(from, to);

        String url = new BocUrlBuilder(config.getBaseUrl())
                .observations(series.toArray(new String[0]))
                .format("json")
                .recent(1)
                .build();

        try {
            String jsonResponse = executeGetRequest(url);

            if (config.isDebugLogging()) {
                log.debug("BOC Response: {}", jsonResponse);
            }

            return objectMapper.readValue(jsonResponse, BocExchangeRateResponse.class);

        } catch (Exception e) {
            log.error("Failed to fetch latest exchange rate from Bank of Canada", e);
            throw new BocApiException("Failed to fetch latest exchange rate", e);
        }
    }

    public BocExchangeRateResponse getHistoricalExchangeRate(String to, String from, LocalDateTime startDate, LocalDateTime endDateTime) {
        List<String> series = BocCurrencyPairResolver.resolveSeries(from, to);

        String url = new BocUrlBuilder(config.getBaseUrl())
                .observations(series.toArray(new String[0]))
                .format("json")
                .startDate(startDate.toLocalDate())
                .endDate(endDateTime.toLocalDate())
                .build();

        try {
            String jsonResponse = executeGetRequest(url);

            if (config.isDebugLogging()) {
                log.debug("BOC Response: {}", jsonResponse);
            }

            return objectMapper.readValue(jsonResponse, BocExchangeRateResponse.class);

        } catch (BocApiException e) {
            log.error("Failed to fetch historical exchange rates from Bank of Canada", e);
            throw e; // or wrap if you really want
        } catch (Exception e) {
            log.error("Failed to parse historical exchange rates from Bank of Canada", e);
        }
        return null;   
    }

    private String executeGetRequest(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check HTTP status
            if (response.statusCode() == 200) {
                return response.body();
            } else if (response.statusCode() == 400) {
                throw new BocApiException("Invalid inputs for the request, please try again.");
            } else if (response.statusCode() == 404) {
                throw new BocApiException("BOC API endpoint not found: " + url);
            } else if (response.statusCode() == 500) {
                throw new BocApiException("An unexpected serverside error has occurred.");
            } else {
                throw new BocApiException(
                        String.format("FMP API error: HTTP %d - %s", response.statusCode(), response.body()));
            }

        } catch (IOException e) {
            throw new BocApiException("Network error calling FMP API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BocApiException("Request interrupted", e);
        }
    }
}