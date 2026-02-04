package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers;

import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.shared.enums.GenericId;

public record PortfolioId(UUID id) implements GenericId {
    public PortfolioId {
        GenericId.validate(id);
    }

    public static PortfolioId newId() {
        return GenericId.generate(PortfolioId::new);
    }

    public static PortfolioId fromString(String value) {
        return GenericId.fromString(PortfolioId::new, value);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}