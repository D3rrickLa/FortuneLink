package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "boc")
public class BankOfCanadaConfigurationProperties {
    private String baseUrl = "https://www.bankofcanada.ca/valet/";

    private int timeoutSeconds = 10;

    private boolean debugLogging = true;

    public void validate() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Bank of Canada base URL cannot be blank");
        }
    }

    @Bean
    @Qualifier("bocHttpClient")
    public HttpClient bocHttpClient(BankOfCanadaConfigurationProperties config) {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
            .build();
    }
}
