package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID>{
    List<TransactionEntity> findByPortfolioIdOrderByTransactionDateDesc(UUID portfolioId);
    
    List<TransactionEntity> findByPortfolioIdAndTransactionDateBetweenOrderByTransactionDateDesc(
        UUID portfolioId, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
}
