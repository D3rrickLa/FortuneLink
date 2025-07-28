package com.laderrco.fortunelink.portfoliomanagment.infrastructure.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;

@Repository
public interface JpaPortfolioRepository extends JpaRepository<Portfolio, UUID>, PortfolioRepository {
    
}
