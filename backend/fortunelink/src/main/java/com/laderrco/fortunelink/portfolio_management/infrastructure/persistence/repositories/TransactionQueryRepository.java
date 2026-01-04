package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;

// pageable -> Spring data interface handling pagination and sortinf for DB queries
// setting it to null = load all at once
// Read-only queries only - NO save() method    
// UPDATE: most of these methods are 'caller concern - controllers / services'
// we are simplifying
public interface TransactionQueryRepository {
    Page<Transaction> find(TransactionQuery query, Pageable pageable);

    long countByPortfolioId(PortfolioId portfolioId);

    long countByAccountId(AccountId accountId);
    
}
