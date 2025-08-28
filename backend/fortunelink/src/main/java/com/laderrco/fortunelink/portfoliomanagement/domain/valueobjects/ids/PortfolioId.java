package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record PortfolioId(UUID portfolioId) {
    public PortfolioId {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
    }

    public static PortfolioId createRandom() {
        return new PortfolioId(UUID.randomUUID());
    }
}