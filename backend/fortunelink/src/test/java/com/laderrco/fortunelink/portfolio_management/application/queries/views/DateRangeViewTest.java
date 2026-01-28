package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DateRangeViewTest {

    @Test
    @DisplayName("allTime() should return 'All time'")
    void allTimeLabel() {
        DateRangeView view = DateRangeView.allTime();

        assertNull(view.startDate());
        assertNull(view.endDate());
        assertEquals("All time", view.getLabel());
    }

    @Test
    @DisplayName("Specific date range should format start and end")
    void specificDateRangeLabel() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");

        DateRangeView view = new DateRangeView(start, end);

        String label = view.getLabel();

        assertTrue(label.contains(start.toString()));
        assertTrue(label.contains(end.toString()));
        assertEquals(start + " to " + end, label);
    }

    @Test
    @DisplayName("Start date only should still format fallback label")
    void startDateOnlyLabel() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");

        DateRangeView view = new DateRangeView(start, null);

        assertEquals(start + " to null", view.getLabel());
    }

    @Test
    @DisplayName("End date only should still format fallback label")
    void endDateOnlyLabel() {
        Instant end = Instant.parse("2024-12-31T23:59:59Z");

        DateRangeView view = new DateRangeView(null, end);

        assertEquals("null to " + end, view.getLabel());
    }
}
