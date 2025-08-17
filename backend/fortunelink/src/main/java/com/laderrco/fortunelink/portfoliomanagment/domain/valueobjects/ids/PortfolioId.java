package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record PortfolioId(UUID portfolioId) {
    public PortfolioId {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
    }
}
