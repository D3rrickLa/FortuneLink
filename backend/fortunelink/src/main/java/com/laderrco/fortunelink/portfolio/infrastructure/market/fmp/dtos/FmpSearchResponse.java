package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Example: https://financialmodelingprep.com/stable/search-symbol?query=AAPL&apikey=xxx Sample
 * Response: [ { "symbol": "AAPL", "name": "Apple Inc.", "currency": "USD", "exchangeFullName":
 * "NASDAQ Global Select", "exchange": "NASDAQ" } ]
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FmpSearchResponse {
  @JsonProperty("symbol")
  private String symbol;

  @JsonProperty("name")
  private String name;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("exchangeFullName")
  private String exchangeFullName;

  @JsonProperty("exchange")
  private String exchange;
}