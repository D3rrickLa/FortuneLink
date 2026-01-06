package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo;

import java.util.List;

/**
 * Response structure for Yahoo Finance Quote API.
 * 
 * API Endpoint: https://query1.finance.yahoo.com/v7/finance/quote?symbols=AAPL
 * 
 * Example Response:
 * {
 *   "quoteResponse": {
 *     "result": [
 *       {
 *         "symbol": "AAPL",
 *         "shortName": "Apple Inc.",
 *         "longName": "Apple Inc.",
 *         "regularMarketPrice": 150.25,
 *         "currency": "USD",
 *         ...
 *       }
 *     ],
 *     "error": null
 *   }
 * }
 */
public record YahooQuoteResponse(
    QuoteResponse quoteResponse
) {
    /**
     * Wrapper for quote results.
     */
    public record QuoteResponse(
        List<YahooQuote> result,
        Object error  // Yahoo returns error object on failure
    ) {
        public boolean hasError() {
            return error != null;
        }
        
        public boolean hasResults() {
            return result != null && !result.isEmpty();
        }
    }
}