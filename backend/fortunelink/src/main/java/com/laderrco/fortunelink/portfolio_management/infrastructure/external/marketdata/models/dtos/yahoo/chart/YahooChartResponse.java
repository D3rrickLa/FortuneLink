package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.chart;

import java.util.List;

/**
 * Response structure for Yahoo Finance Chart API.
 * 
 * API Endpoint: https://query1.finance.yahoo.com/v8/finance/chart/AAPL?period1=X&period2=Y
 * 
 * Used for historical price data.
 * 
 * Example Response:
 * {
 *   "chart": {
 *     "result": [
 *       {
 *         "meta": {
 *           "currency": "USD",
 *           "symbol": "AAPL",
 *           "exchangeName": "NMS"
 *         },
 *         "timestamp": [1640000000, 1640086400, ...],
 *         "indicators": {
 *           "quote": [
 *             {
 *               "close": [150.25, 151.50, ...],
 *               "open": [149.00, 150.75, ...],
 *               "high": [151.00, 152.00, ...],
 *               "low": [148.50, 150.00, ...]
 *             }
 *           ]
 *         }
 *       }
 *     ],
 *     "error": null
 *   }
 * }
 */
public record YahooChartResponse(
    ChartData chart
) {
    /**
     * Wrapper for chart results.
     */
    public record ChartData(
        List<YahooChartResult> result,
        Object error
    ) {
        public boolean hasError() {
            return error != null;
        }
        
        public boolean hasResults() {
            return result != null && !result.isEmpty();
        }
    }
}