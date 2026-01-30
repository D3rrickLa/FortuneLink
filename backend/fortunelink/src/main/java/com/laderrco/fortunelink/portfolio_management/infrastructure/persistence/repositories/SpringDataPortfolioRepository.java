
package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;

public interface SpringDataPortfolioRepository extends JpaRepository<PortfolioEntity, UUID>{
    Optional<PortfolioEntity> findByUserId(UUID userId);
    Optional<PortfolioEntity> findByIdAndUserId(UUID portfolioId, UUID userId);
    List<PortfolioEntity> findAllByUserId(UUID userId);
    Long countByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}