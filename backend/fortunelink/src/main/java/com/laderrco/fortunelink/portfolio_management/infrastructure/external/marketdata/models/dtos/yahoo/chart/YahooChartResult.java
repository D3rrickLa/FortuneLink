package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.chart;

import java.util.List;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.YahooBaseData;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.YahooIndicators;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Chart data for a single symbol.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class YahooChartResult extends YahooBaseData {
    private List<Long> timestamp; // Unix timestamps (seconds)
    private YahooIndicators indicators;

    /**
     * Check if chart has valid data.
     */
    public boolean hasData() {
        return timestamp != null && !timestamp.isEmpty()
                && indicators != null
                && indicators.hasQuoteData();
    }

    /**
     * Get the number of data points.
     */
    public int getDataPointCount() {
        return timestamp != null ? timestamp.size() : 0;
    }

    public boolean isMeta() {
        return super.getCurrency() == null && super.getExchange() == null && super.getSymbol() == null;
    }
}
