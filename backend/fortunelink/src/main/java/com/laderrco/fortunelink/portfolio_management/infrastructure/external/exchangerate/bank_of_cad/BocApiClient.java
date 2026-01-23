package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.BankOfCanadaConfigurationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BocApiClient {

    private final BankOfCanadaConfigurationProperties config;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    public BocApiClient(BankOfCanadaConfigurationProperties config,  @Qualifier("defaultObjectMapper") ObjectMapper objectMapper, HttpClient httpClient) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;

        log.info("Bank of Canada API Client initialized with base URL: {}", config.getBaseUrl());
    }

    public void getLatestExchangeRate(String to) {

    }
}