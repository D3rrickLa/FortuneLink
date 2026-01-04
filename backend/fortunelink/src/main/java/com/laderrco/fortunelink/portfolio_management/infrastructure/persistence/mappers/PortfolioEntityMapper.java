package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;

public interface PortfolioEntityMapper {
    public PortfolioEntity toEntity(Portfolio portfolio);
    public Portfolio toDomain(PortfolioEntity entity);
    public void updateEntityFromDomain(Portfolio domain, PortfolioEntity entity);
}
