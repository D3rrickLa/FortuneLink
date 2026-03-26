package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;

@Repository
public interface JpaPortfolioRepository extends JpaRepository<Portfolio, UUID> {
    // Ownership-scoped fetch WITH full graph. Used when session will close before mapping
    @EntityGraph(attributePaths = { "accounts", "accounts.positions", "accounts.realizedGains" })
    Optional<Portfolio> findWithAccountsByIdAndUserId(UUID id, UUID userId);
}
