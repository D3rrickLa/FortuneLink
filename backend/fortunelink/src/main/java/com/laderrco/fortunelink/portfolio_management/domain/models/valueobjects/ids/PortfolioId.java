package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.GenericId;

public record PortfolioId(UUID portfolioId) implements GenericId {
        public PortfolioId {
        portfolioId = GenericId.validate(portfolioId);
    }

    public static PortfolioId randomId() {
        return GenericId.random(PortfolioId::new);
    }
}
