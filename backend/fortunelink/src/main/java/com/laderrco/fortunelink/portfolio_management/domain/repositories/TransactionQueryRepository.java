package com.laderrco.fortunelink.portfolio_management.domain.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;

public interface TransactionQueryRepository {
    List<Transaction> findByPortfolioId(PortfolioId portfolioId, Pageable pageable);
    List<Transaction> findByDateRange(PortfolioId portfolioId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    List<Transaction> findByAccountId(AccountId accountId, Pageable pageable);
    long countByPortfolioId(PortfolioId portfolioId);
    // Read-only queries only - NO save() method    
}
