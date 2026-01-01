package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapper;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class TransactionQueryRepositoryImpl implements TransactionQueryRepository {
    private final JpaTransactionRepository jpaRepository;
    private final TransactionEntityMapper mapper;

    @Override
    public List<Transaction> findByPortfolioId(PortfolioId portfolioId, Pageable pageable) {
        List<TransactionEntity> entities = jpaRepository.findByPortfolioId(
            portfolioId.portfolioId(), 
            pageable
        );
        return entities.stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<Transaction> findByAccountId(AccountId accountId, Pageable pageable) {
        Objects.requireNonNull(pageable);
        Page<TransactionEntity> entityPage = jpaRepository.findByAccountId(
            accountId.accountId(), 
            pageable
        );
        List<Transaction> transactions = Objects.requireNonNull(entityPage.getContent().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList()));
        
        return new PageImpl<>(
            transactions, 
            pageable, 
            entityPage.getTotalElements()
        );
    }
    
    @Override
    public List<Transaction> findByDateRange(
            PortfolioId portfolioId, 
            LocalDateTime start, 
            LocalDateTime end, 
            Pageable pageable) {
        
        Page<TransactionEntity> entityPage = jpaRepository.findByPortfolioIdWithFilters(
            portfolioId.portfolioId(),
            null, // no type filter
            start,
            end,
            pageable
        );
        
        return entityPage.getContent().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public long countByPortfolioId(PortfolioId portfolioId) {
        return jpaRepository.countByPortfolioId(portfolioId.portfolioId());
    }
    
    @Override
    public long countByAccountId(AccountId accountId) {
        return jpaRepository.countByAccountId(accountId.accountId());
    }
    
    @Override
    public Page<Transaction> findByPortfolioIdAndTransactionType(
            PortfolioId portfolioId, 
            TransactionType transactionType, 
            Pageable pageable) {
        Objects.requireNonNull(pageable);

        Page<TransactionEntity> entityPage = jpaRepository.findByPortfolioIdWithFilters(
            portfolioId.portfolioId(),
            transactionType.name(),
            null,
            null,
            pageable
        );
        
        List<Transaction> transactions = Objects.requireNonNull(entityPage.getContent().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList()));
        
        return new PageImpl<>(
            transactions, 
            pageable, 
            entityPage.getTotalElements()
        );
    }
    
    @Override
    public Page<Transaction> findByFilters(
            PortfolioId portfolioId,
            TransactionType transactionType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Set<String> assetSymbols,
            Pageable pageable) {
        Objects.requireNonNull(pageable);
        
        // For now, delegate to portfolio filters
        // Asset symbol filtering would require additional query customization
        Page<TransactionEntity> entityPage = jpaRepository.findByPortfolioIdWithFilters(
            portfolioId.portfolioId(),
            transactionType != null ? transactionType.name() : null,
            startDate,
            endDate,
            pageable
        );
        
        List<Transaction> transactions = Objects.requireNonNull(entityPage.getContent().stream()
            .map(mapper::toDomain)
            .filter(t -> assetSymbols == null || assetSymbols.isEmpty() || 
                (t.getAssetIdentifier().getPrimaryId() != null && assetSymbols.contains(t.getAssetIdentifier().getPrimaryId())))
            .collect(Collectors.toList()
        ));

        
        return new PageImpl<>(
            transactions, 
            pageable, 
            entityPage.getTotalElements()
        );
    }
    
    @Override
    public Page<Transaction> findByPortfolioIdAndFilters(PortfolioId portfolioId, TransactionType transactionType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Objects.requireNonNull(pageable);
        Page<TransactionEntity> entityPage = jpaRepository.findByPortfolioIdWithFilters(
            portfolioId.portfolioId(),
            transactionType != null ? transactionType.name() : null,
            startDate,
            endDate,
            pageable
        );
        
        List<Transaction> transactions = Objects.requireNonNull(entityPage.getContent().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList()));
        
        return new PageImpl<>(
            transactions, 
            pageable, 
            entityPage.getTotalElements()
        );
    }
    
    @Override
    public Page<Transaction> findByAccountIdAndFilters(
            AccountId accountId,
            TransactionType transactionType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        Objects.requireNonNull(pageable); 
        Page<TransactionEntity> entityPage = jpaRepository.findByAccountIdWithFilters(
            accountId.accountId(),
            transactionType != null ? transactionType.name() : null,
            startDate,
            endDate,
            pageable
        );
        
        List<Transaction> transactions = Objects.requireNonNull(entityPage.getContent().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList()));
        
        return new PageImpl<>(
            transactions, 
            pageable, 
            entityPage.getTotalElements()
        );
    }
    
}
