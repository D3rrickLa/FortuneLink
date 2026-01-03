package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.PortfolioEntityMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaPortfolioRepository implements PortfolioRepository {

private final SpringDataPortfolioRepository jpaRepo; // The standard JpaRepository<PortfolioEntity, UUID>
    private final PortfolioEntityMapper portfolioMapper;

    @Override
    public Optional<Portfolio> findById(PortfolioId id) {
        UUID portfolioUuid = id.portfolioId();
        Objects.requireNonNull(portfolioUuid, "Portfolio ID cannot be null");
        return jpaRepo.findById(portfolioUuid).map(portfolioMapper::toDomain);
    }

    @Override
    public Optional<Portfolio> findByUserId(UserId userId) {
        return jpaRepo.findByUserId(userId.userId()).map(portfolioMapper::toDomain);
    }

    @Override
    public Portfolio save(Portfolio portfolio) {
        UUID portfolioUuid = portfolio.getPortfolioId().portfolioId();
        Objects.requireNonNull(portfolioUuid, "Portfolio ID cannot be null");
        // 1. Load existing or create fresh
        PortfolioEntity entity = jpaRepo.findById(portfolioUuid)
            .orElseGet(() -> {
                PortfolioEntity newEntity = new PortfolioEntity(portfolio.getPortfolioId().portfolioId(), portfolio.getUserId().userId());
                return newEntity;
            }
        );

        // 2. Map Domain state onto the Entity
        Objects.requireNonNull(entity, "Entity cannot be null");
        portfolioMapper.updateEntityFromDomain(portfolio, entity);

        // 3. Save and return the mapped result
        PortfolioEntity saved = jpaRepo.save(entity);
        return portfolioMapper.toDomain(saved);
    }

    @Override
    public void delete(PortfolioId id) {
        UUID portfolioUuid = id.portfolioId();
        Objects.requireNonNull(portfolioUuid, "Portfolio ID cannot be null");
        jpaRepo.deleteById(portfolioUuid);
    }
    
}
