package com.laderrco.fortunelink.portfolio_management.application.services;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionQuery;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionQueryService {
    private final TransactionQueryRepository repository;

    public Page<Transaction> getPortfolioTransactions(
            PortfolioId portfolioId,
            Pageable pageable) {

        TransactionQuery query = new TransactionQuery(
                portfolioId.portfolioId(),
                null, null, null, null, null);

        return repository.find(query, pageable);
    }

    public Page<Transaction> getPortfolioTransactionsByDateRange(
            PortfolioId portfolioId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {

        TransactionQuery query = new TransactionQuery(
                portfolioId.portfolioId(),
                null,
                null,
                start,
                end,
                null);

        return repository.find(query, pageable);
    }

    public Page<Transaction> findByPortfolioIdAndFilters(
            PortfolioId portfolioId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        TransactionQuery query = new TransactionQuery(
                portfolioId.portfolioId(), // portfolio
                null, // account
                type,
                startDate,
                endDate,
                null // asset symbols, optional
        );

        return repository.find(query, pageable);
    }

    public Page<Transaction> getAccountTransactionsByType(
            AccountId accountId,
            TransactionType type,
            Pageable pageable) {

        TransactionQuery query = new TransactionQuery(
                null,
                accountId.accountId(),
                type,
                null,
                null,
                null);

        return repository.find(query, pageable);
    }

    public Page<Transaction> findByAccountIdAndFilters(
            AccountId accountId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        TransactionQuery query = new TransactionQuery(
                null, // portfolio
                accountId.accountId(), // account
                type,
                startDate,
                endDate,
                null // asset symbols, optional
        );

        return repository.find(query, pageable);
    }

    public Page<Transaction> getTransactionsWithFilters(
            PortfolioId portfolioId,
            TransactionType type,
            LocalDateTime start,
            LocalDateTime end,
            Set<String> symbols,
            Pageable pageable) {

        TransactionQuery query = new TransactionQuery(
                portfolioId.portfolioId(),
                null,
                type,
                start,
                end,
                symbols);

        return repository.find(query, pageable);
    }
}
