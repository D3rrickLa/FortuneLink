
package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;

public interface SpringDataPortfolioRepository extends JpaRepository<PortfolioEntity, UUID>{
    Optional<PortfolioEntity> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}