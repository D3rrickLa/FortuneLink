package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for Financial Modeling Prep (FMP) API.
 * 
 * Maps to application.yml:
 * fmp:
 * api-key: ${FMP_API_KEY}
 * base-url: https://financialmodelingprep.com/api/v3
 * timeout-seconds: 10
 * 
 * Free Tier Limits:
 * - 250 requests per day
 * - Real-time quotes for stocks, ETFs, crypto
 * - Company profiles and financial data
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fmp")
public class FmpConfigurationProperties {
    /**
     * FMP API key from environment variable.
     * Get yours at: https://financialmodelingprep.com/developer/docs/
     */
    private String apiKey;

    /**
     * Base URL for FMP API
     * Default: https://financialmodelingprep.com/
     */
    private String baseUrl = "https://financialmodelingprep.com/stable/";

    /**
     * HTTP timeout in seconds.
     * Default: 10 seconds
     */
    private int timeoutSeconds = 10;

    /**
     * Enable request/response logging for debugging.
     * Default: false (set true in dev, false in prod)
     */
    private boolean debugLogging = true;

    /**
     * Validate configuration on startup.
     */
    public void validate() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "FMP API key is required. Set environment variable FMP_API_KEY or configure fmp.api-key in application.yml");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("FMP base URL cannot be blank");
        }
    }

    @Bean
    public HttpClient fmpHttpClient(FmpConfigurationProperties config) {
    return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
            .build();
    }
}
