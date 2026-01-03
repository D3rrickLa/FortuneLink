package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

/**
 * Implementation of query repository.
 * This is purely for READ operations, optimizing query performance.
 */
@Repository
@AllArgsConstructor
public class TransactionQueryRepositoryImpl implements TransactionQueryRepository {
    private final JpaTransactionRepository jpaRepository;
    private final TransactionEntityMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public Page<Transaction> find(TransactionQuery query, Pageable pageable) {
        Objects.requireNonNull(pageable);

        Page<TransactionEntity> entityPage = jpaRepository.findWithFilters(
                query.portfolioId(),
                query.accountId(),
                query.transactionType() != null ? query.transactionType() : null,
                query.startDate().toInstant(ZoneOffset.UTC),
                query.endDate().toInstant(ZoneOffset.UTC),
                pageable);

        Page<Transaction> mappedPage = mapPage(entityPage, pageable);

        // In-memory filtering ONLY when unavoidable
        if (query.assetSymbols() == null || query.assetSymbols().isEmpty()) {
            return mappedPage;
        }

        List<Transaction> filtered = mappedPage.getContent().stream()
                .filter(t -> t.getAssetIdentifier().getPrimaryId() != null &&
                        query.assetSymbols().contains(
                                t.getAssetIdentifier().getPrimaryId()))
                .toList();

        return new PageImpl<>(
                filtered,
                pageable,
                filtered.size() // important: do NOT lie about totals
        );
    }

    @Override
    @Transactional
    public long countByPortfolioId(PortfolioId portfolioId) {
        return jpaRepository.countByPortfolioId(portfolioId.portfolioId());
    }

    @Override
    @Transactional
    public long countByAccountId(AccountId accountId) {
        return jpaRepository.countByAccountId(accountId.accountId());
    }

    private Page<Transaction> mapPage(
            Page<TransactionEntity> entityPage,
            Pageable pageable) {

        List<Transaction> transactions = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageImpl<>(
                transactions,
                pageable,
                entityPage.getTotalElements());
    }

}
