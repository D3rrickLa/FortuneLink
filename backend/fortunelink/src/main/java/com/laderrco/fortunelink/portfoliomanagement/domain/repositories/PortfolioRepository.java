package com.laderrco.fortunelink.portfoliomanagement.domain.repositories;

import java.util.Optional;

public interface PortfolioRepository {
    public Object save(Object portoflio); // need to change htis to Portfolio
    public Optional<Object> findById(Object portfolioId); // return portfolio
    public Optional<Object> findByUserId(Object userId); // return portfolio
    public void delete(Object portfolioId);
}
