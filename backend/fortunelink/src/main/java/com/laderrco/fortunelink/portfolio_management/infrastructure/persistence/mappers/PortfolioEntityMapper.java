package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;

// We are mapping/converting between domai nobjects, with has things like Money and Asset to 
// JPA entities whtih contains primitives only and vice versa
// The portfolio rpeo will call this, then the mapper, this, will delegate the heavy lifting to the other mappers if needs be (i.e. asset)
@Component
public class PortfolioEntityMapper {
    public PortfolioEntity toEntity(Portfolio portfolio) {
        // Map Portfolio domain object → PortfolioEntity
        // Handle nested Account and Asset mappings
        return null;
    }
    
    public Portfolio toDomain(PortfolioEntity entity) {
        // Map PortfolioEntity → Portfolio domain object
        // Reconstruct all value objects (Money, Quantity, etc.)
        return null;
    }   
}
