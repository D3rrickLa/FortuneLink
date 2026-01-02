package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class PortfolioEntityTest {
    @Test
    void testConstructor() {
        UUID portfolioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PortfolioEntity entity = new PortfolioEntity(portfolioId, userId);

        assertEquals(portfolioId, entity.getId());
        assertEquals(userId, entity.getUserId());
    }
}
