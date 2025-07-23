package com.laderrco.fortunelink.portfoliomanagment.domain.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;

public interface PortfolioRepository {
    public Optional<Portfolio> findById(UUID id);
    public List<Portfolio> findByUserId(UUID id);
    public Portfolio save(Portfolio portfolio);
    public void delete(Portfolio portfolio);
    List<Portfolio> findAll();
    
} 

