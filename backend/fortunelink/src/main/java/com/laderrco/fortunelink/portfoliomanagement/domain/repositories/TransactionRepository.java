package com.laderrco.fortunelink.portfoliomanagement.domain.repositories;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.TransactionId;

// impelments as a jpa repo in infra layer
public interface TransactionRepository {
    public Transaction save(Transaction transaction);
    public Optional<Transaction> findById(TransactionId TransactionId);
    public List<Transaction> findByPortfolioId(PortfolioId portfolioId);
    public List<Transaction> findByPortfolioId(PortfolioId portfolioId, Instant start, Instant end);
    public List<Transaction> findRecentByPortfolioId(PortfolioId portfolioId, int limit);
    public List<Transaction> findByDateRange(PortfolioId portfolioId, LocalDateTime start, LocalDateTime end);
}
