package com.laderrco.fortunelink.portfoliomanagment.domain.repositories;

import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;

public interface PortfolioRepository {
    public Portfolio findById(UUID id);
    public List<Portfolio> findByUserId(UUID id);
    public void save(Portfolio portfolio);
    public void delete(Portfolio portfolio);
    List<Portfolio> findAll();
    
} 

