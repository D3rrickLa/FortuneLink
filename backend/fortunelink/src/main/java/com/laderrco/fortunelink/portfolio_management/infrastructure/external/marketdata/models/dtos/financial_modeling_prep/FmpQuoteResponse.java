package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.financial_modeling_prep;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * FMP API response for real-time quote endpoint.
 * 
 * Endpoint: GET /quote/{symbol}
 * Example: https://financialmodelingprep.com/api/v3/quote/AAPL?apikey=xxx
 * 
 * Sample Response:
 * [
	{
		"symbol": "AAPL",
		"name": "Apple Inc.",
		"price": 232.8,
		"changePercentage": 2.1008,
		"change": 4.79,
		"volume": 44489128,
		"dayLow": 226.65,
		"dayHigh": 233.13,
		"yearHigh": 260.1,
		"yearLow": 164.08,
		"marketCap": 3500823120000,
		"priceAvg50": 240.2278,
		"priceAvg200": 219.98755,
		"exchange": "NASDAQ",
		"open": 227.2,
		"previousClose": 228.01,
		"timestamp": 1738702801
	}
 * ]
 * 
 * ^^^ THIS IS WHAT WE WANT, BUT WE CAN' CUZ BROKE
 * INSTEAD WE ARE USING THE COMPANY INFO - ANOTHER CLASS
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpQuoteResponse {
    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("name")
    private String name;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("changesPercentage")
    private BigDecimal changesPercentage;

    @JsonProperty("change")
    private BigDecimal change;

    @JsonProperty("dayLow")
    private BigDecimal dayLow;

    @JsonProperty("dayHigh")
    private BigDecimal dayHigh;

    @JsonProperty("yearHigh")
    private BigDecimal yearHigh;

    @JsonProperty("yearLow")
    private BigDecimal yearLow;

    @JsonProperty("marketCap")
    private Long marketCap;

    @JsonProperty("priceAvg50")
    private BigDecimal priceAvg50;

    @JsonProperty("priceAvg200")
    private BigDecimal priceAvg200;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("open")
    private BigDecimal open;

    @JsonProperty("previousClose")
    private BigDecimal previousClose;

    @JsonProperty("timestamp")
    private Long timestamp;
}
