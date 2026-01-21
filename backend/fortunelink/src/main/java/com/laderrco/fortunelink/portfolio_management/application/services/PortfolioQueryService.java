package com.laderrco.fortunelink.portfolio_management.application.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio_management.application.mappers.AllocationMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionMapper;
import com.laderrco.fortunelink.portfolio_management.application.models.TransactionSearchCriteria;
import com.laderrco.fortunelink.portfolio_management.application.queries.AnalyzeAllocationQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewNetWorthQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewPerformanceQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AllocationView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.NetWorthView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PerformanceView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionHistoryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.assemblers.PortfolioViewAssembler;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.AllArgsConstructor;

/**
 * Application service for portfolio query operations.
 * 
 * This service orchestrates read-only portfolio operations by:
 * - Loading portfolios from the repository
 * - Coordinating with domain services for calculations
 * - Using TransactionQueryService for transaction queries
 * - Mapping domain objects to response DTOs
 * 
 * All methods are read-only (no state changes to aggregates).
 * 
 * NOTE: one portoflio by policy, not by model - pirmary means only this exists
 */
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService {

    // Repositories
    private final PortfolioRepository portfolioRepository;

    // Application Services
    private final TransactionQueryService transactionQueryService;

    // Domain Services
    private final PerformanceCalculationService performanceCalculationService;
    private final AssetAllocationService assetAllocationService;
    private final PortfolioValuationService portfolioValuationService;

    // View Assemblers
    private final PortfolioViewAssembler portfolioAssembler;

    /**
     * Retrieves a portfolio by its unique identifier.
     *
     * Intended for explicit portfolio access (admin, deep links,
     * future multi-portfolio support).
     */
    public PortfolioView getPortfolioById(GetPortfolioByIdQuery query) {
        Objects.requireNonNull(query, "GetPortfolioByIdQuery cannot be null");

        Portfolio portfolio = portfolioRepository.findById(query.id())
                .orElseThrow(() -> new PortfolioNotFoundException("Cannot find portfolio with id " + query.id()));

        return portfolioAssembler.assemblePortfolioView(portfolio);
    }

    /**
     * Retrieves the portfolio owned by the given user.
     *
     * NOTE: Users are currently limited to a single portfolio by
     * application policy. This method is forward-compatible with
     * multi-portfolio support.
     */
    public PortfolioView getUserPortfolioView(GetPortfolioSummaryQuery query) {
        Objects.requireNonNull(query, "GetPortfolioSummaryQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.userId());
        return portfolioAssembler.assemblePortfolioView(portfolio);
    }

    /**
     * Returns summary views of all portfolios owned by a user.
     *
     * NOTE: Currently returns a single entry due to subscription policy.
     */
    public List<PortfolioSummaryView> getUserPortfolioSummaries(GetPortfoliosByUserIdQuery query) {
        Objects.requireNonNull(query, "GetPortfoliosByUserIdQuery cannot be null");

        return portfolioRepository.findAllByUserId(query.id()).stream()
                .map(portfolioAssembler::assemblePortfolioSummaryView)
                .toList();
    }

    /**
     * Calculates net worth for a user's portfolio.
     *
     * Net Worth = Total Assets - Total Liabilities
     */
    public NetWorthView getNetWorth(ViewNetWorthQuery query) {
        Objects.requireNonNull(query, "ViewNetWorthQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.userId());
        Instant calculationDate = query.asOfDate() != null
                ? query.asOfDate()
                : Instant.now();

        Money totalAssets = portfolioValuationService.calculateTotalValue(portfolio, calculationDate);

        // TODO: integrate liabilities via ACL (Loan / Debt context)
        Money totalLiabilities = Money.ZERO(portfolio.getPortfolioCurrencyPreference());

        Money netWorth = totalAssets.subtract(totalLiabilities);

        return new NetWorthView(
                totalAssets,
                totalLiabilities,
                netWorth,
                calculationDate,
                totalAssets.currency());
    }

    /**
     * Calculates portfolio performance metrics over a time range.
     */
    public PerformanceView getPortfolioPerformance(ViewPerformanceQuery query) {
        Objects.requireNonNull(query, "ViewPerformanceQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.userId());

        TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
                .portfolioId(portfolio.getPortfolioId())
                .startDate(LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC))
                .endDate(LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC))
                .build();

        // Performance calculations require the full transaction set
        List<Transaction> transactions = transactionQueryService.getAllTransactions(criteria);

        Percentage totalReturn = performanceCalculationService.calculateTotalReturn(portfolio);

        Money realizedGains = performanceCalculationService.calculateRealizedGains(portfolio, transactions);

        Money unrealizedGains = performanceCalculationService.calculateUnrealizedGains(portfolio);

        Percentage timeWeightedReturn = performanceCalculationService.calculateTimeWeightedReturn(portfolio);

        long daysBetween = ChronoUnit.DAYS.between(query.startDate(), query.endDate());

        double years = daysBetween / 365.25;
        Percentage annualizedReturn = totalReturn.annualize(years);

        String period = query.startDate() + " to " + query.endDate();

        return new PerformanceView(
                totalReturn,
                annualizedReturn,
                realizedGains,
                unrealizedGains,
                timeWeightedReturn,
                null, // money-weighted return (future)
                period);
    }

    /**
     * Analyzes asset allocation by type, account, or currency.
     */
    public AllocationView getAssetAllocation(AnalyzeAllocationQuery query) {
        Objects.requireNonNull(query, "AnalyzeAllocationQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.userId());
        Instant asOfDate = query.asOfDate() != null
                ? query.asOfDate()
                : Instant.now();

        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, asOfDate);

        return switch (query.allocationType()) {
            case BY_TYPE -> AllocationMapper.toResponseFromAssetType(
                    assetAllocationService.calculateAllocationByType(portfolio, asOfDate),
                    totalValue,
                    asOfDate);
            case BY_ACCOUNT -> AllocationMapper.toResponseFromAccountType(
                    assetAllocationService.calculateAllocationByAccount(portfolio, asOfDate),
                    totalValue,
                    asOfDate);
            case BY_CURRENCY -> AllocationMapper.toResponseFromCurrency(
                    assetAllocationService.calculateAllocationByCurrency(portfolio, asOfDate),
                    totalValue,
                    asOfDate);
        };
    }

    /**
     * Retrieves paginated transaction history with optional filters.
     */
    public TransactionHistoryView getTransactionHistory(GetTransactionHistoryQuery query) {
        Objects.requireNonNull(query, "GetTransactionHistoryQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.userId());

        LocalDateTime startDate = query.startDate() != null
                ? LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC)
                : null;

        LocalDateTime endDate = query.endDate() != null
                ? LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC)
                : null;

        TransactionSearchCriteria.TransactionSearchCriteriaBuilder criteriaBuilder = TransactionSearchCriteria.builder()
                .portfolioId(portfolio.getPortfolioId())
                .transactionType(query.transactionType())
                .startDate(startDate)
                .endDate(endDate);

        if (query.accountId() != null) {
            criteriaBuilder.accountId(query.accountId());
        }

        Page<Transaction> transactionPage = transactionQueryService.queryTransactions(
                criteriaBuilder.build(),
                query.pageNumber() - 1,
                query.pageSize());

        List<TransactionView> transactions = TransactionMapper.toResponseList(transactionPage.getContent());

        String dateRange = query.startDate() != null && query.endDate() != null
                ? query.startDate() + " to " + query.endDate()
                : "All time";

        return new TransactionHistoryView(
                transactions,
                (int) transactionPage.getTotalElements(),
                query.pageNumber(),
                query.pageSize(),
                dateRange);
    }

    /**
     * Retrieves summary information for a specific account.
     */
    public AccountView getAccountSummary(GetAccountSummaryQuery query) {
        Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.userId());
        Account account = portfolio.getAccount(query.accountId());

        return portfolioAssembler.assembleAccountView(account);
    }

    /**
     * Loads the portfolio owned by the given user.
     *
     * Centralizes the current "one portfolio per user" policy and
     * provides a single seam for future multi-portfolio evolution.
     */
    private Portfolio loadUserPortfolio(UserId userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException(userId));
    }
}
