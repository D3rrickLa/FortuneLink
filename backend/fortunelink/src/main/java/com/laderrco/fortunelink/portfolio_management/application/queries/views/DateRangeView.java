package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import java.time.Instant;

public record DateRangeView(Instant startDate, Instant endDate) {
    public static DateRangeView allTime() {
        return new DateRangeView(null, null);
    }

    public String getLabel() {
        if (startDate == null && endDate == null) {
            return "All time";
        }
        // Fallback for specific ranges
        return String.format("%s to %s", startDate, endDate);
    }
}
