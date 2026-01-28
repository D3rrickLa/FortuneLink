package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class PortfolioSummaryViewTest {
    @Test
    void testContstructor() {
        PortfolioId id = PortfolioId.randomId();
        String name = "Name";
        Money totalValue = Money.ZERO("USD");
        Instant time = Instant.now();
        PortfolioSummaryView summaryView = new PortfolioSummaryView(
                id,
                name, totalValue, time);

        assertAll(
            () -> assertEquals(id, summaryView.id()),
            () -> assertEquals(name, summaryView.name()),
            () -> assertEquals(totalValue, summaryView.totalValue()),
            () -> assertEquals(time, summaryView.lastUpdated())
        );
    }

}
