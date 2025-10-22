package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.UUID;

public record PortfolioId(UUID transactionId) implements GenericId {
    public PortfolioId {
        transactionId = GenericId.validate(transactionId);
    }

    public static PortfolioId randomId() {
        return GenericId.random(PortfolioId::new);
    }
    
}
