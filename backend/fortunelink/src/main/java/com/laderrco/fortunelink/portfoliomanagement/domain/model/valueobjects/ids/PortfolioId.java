package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record PortfolioId(UUID portfolioId) {
    public PortfolioId {
        Objects.nonNull(portfolioId);
    }

    public static PortfolioId randomId() {
        return new PortfolioId(UUID.randomUUID());
    }

}
