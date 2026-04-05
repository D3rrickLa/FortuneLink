package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Financial Modeling Prep (FMP) API.
 * <p>
 * Maps to application.yml: fmp: api-key: ${FMP_API_KEY} base-url:
 * https://financialmodelingprep.com/api/v3 timeout-seconds: 10
 * <p>
 * Free Tier Limits: - 250 requests per day - Real-time quotes for stocks, ETFs, crypto - Company
 * profiles and financial data
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fmp")
public class FmpClientConfig {

  private String apiKey;
  private String baseUrl = "https://financialmodelingprep.com/api/v3";
  private int timeoutSeconds = 10;
  private boolean debugLogging = false;

  /**
   * Validate on startup.
   */
  @PostConstruct
  public void validate() {
    if (apiKey == null || apiKey.isBlank() || "YOUR_API_KEY".equals(apiKey)) {
      throw new IllegalStateException("FMP API key is missing! Set FMP_API_KEY env var.");
    }
  }
}