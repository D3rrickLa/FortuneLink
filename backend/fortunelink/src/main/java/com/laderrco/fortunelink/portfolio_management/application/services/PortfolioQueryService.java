package com.laderrco.fortunelink.portfolio_management.application.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionHistoryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.assemblers.PortfolioViewAssembler;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
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
    private final MarketDataService marketDataService;
    private final PerformanceCalculationService performanceCalculationService;
    private final AssetAllocationService assetAllocationService;
    private final PortfolioValuationService portfolioValuationService;
    
    // Mappers
    private final PortfolioViewAssembler portfolioAssembler;
    
    // Future: LiabilityQueryService liabilityQueryService // ACL interface for Loan Management context


    // Standard 'get query' for the portoflio
    /**
     * Get portfolio by ID.
     */
    @Transactional(readOnly = true)
    public Portfolio getPortfolio(GetPortfolioByIdQuery query) {
        return portfolioRepository.findById( query.id())
            .orElseThrow(() -> new PortfolioNotFoundException("Cannot find portfolio with id " + query.id()));
    }
    
    /**
     * Get all portfolios for a user.
     */
    @Transactional(readOnly = true)
    public List<Portfolio> getUserPortfolios(GetPortfoliosByUserIdQuery query) {        
        return portfolioRepository.findAllByUserId(query.id());
    }


    /**
     * Calculate net worth for a user's portfolio.
     * 
     * Net Worth = Total Assets - Total Liabilities
     * 
     * @param query Contains userId and optional asOfDate
     * @return Net worth response with breakdown
     */
    public NetWorthView getNetWorth(ViewNetWorthQuery query) {
        Objects.requireNonNull(query, "ViewNetWorthQuery cannot be null");
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        Instant calculationDate = query.asOfDate() != null ? query.asOfDate() : Instant.now();

        Money totalAssets = portfolioValuationService.calculateTotalValue(portfolio, calculationDate);

        // TODO: When Loan Management context is implemented, fetch liabilities via ACL
        // Example: Money totalLiabilities = liabilityQueryService.getTotalLiabilities(
        //     query.userId(), 
        //     portfolio.getPortfolioCurrencyPreference()
        // );
        Money totalLiabilities = Money.ZERO(portfolio.getPortfolioCurrencyPreference());

        Money netWorth = totalAssets.subtract(totalLiabilities);

        return new NetWorthView(totalAssets, totalLiabilities, netWorth, calculationDate, totalAssets.currency());

    }

    /**
     * Calculate portfolio performance metrics over a time period.
     * 
     * This method retrieves ALL transactions in the date range (unpaged)
     * because performance calculations need the complete dataset.
     * 
     * @param query Contains userId, startDate, and endDate
     * @return Performance metrics including returns and gains
     */
    public PerformanceView getPortfolioPerformance(ViewPerformanceQuery query) {
        Objects.requireNonNull(query, "ViewPerformanceQuery cannot be null");
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));

        // Build search criteria using the builder pattern
        TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
            .portfolioId(portfolio.getPortfolioId())
            .startDate(LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC))
            .endDate(LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC))
            .build();
        
            
            // Get ALL transactions in range (unpaged) - needed for performance calculations
        List<Transaction> transactions = transactionQueryService.getAllTransactions(criteria);
    
        // Calculate perofrmance metrics 
        Percentage totalReturn = performanceCalculationService.calculateTotalReturn(portfolio);
        
        Money realizedGains = performanceCalculationService.calculateRealizedGains(portfolio, transactions);
        
        Money unrealizedGains = performanceCalculationService.calculateUnrealizedGains(portfolio);
        
        Percentage timeWeightedReturn = performanceCalculationService.calculateTimeWeightedReturn(portfolio);

        // Calculate annualized return
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(query.startDate(), query.endDate());
        double years = daysBetween / 365.25;
        Percentage annualizedReturn = totalReturn.annualize(years);
        String period = query.startDate() + " to " + query.endDate();

        return new PerformanceView(
            totalReturn,
            annualizedReturn,
            realizedGains,
            unrealizedGains,
            timeWeightedReturn,
            null, // moneyWeightedReturn - to be implemented
            period
        ); 
    }

    /**
     * Analyze asset allocation by type, account, or currency.
     * 
     * @param query Contains userId, allocationType, and optional asOfDate
     * @return Allocation breakdown as percentages
     */
    public AllocationView getAssetAllocation(AnalyzeAllocationQuery query) {
        Objects.requireNonNull(query, "AnalyzeAllocationQuery cannot be null");
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));

        Instant asOfDate = query.asOfDate() != null ? query.asOfDate() : Instant.now(); // query as of date doesn't need a check 

        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, asOfDate);
        
        return switch (query.allocationType()) {
            case BY_TYPE -> {
                Map<AssetType, Money> allocations = assetAllocationService
                    .calculateAllocationByType(portfolio, asOfDate);
                yield AllocationMapper.toResponseFromAssetType(allocations, totalValue, asOfDate);
            }
            case BY_ACCOUNT -> {
                Map<AccountType, Money> allocations = assetAllocationService
                    .calculateAllocationByAccount(portfolio, asOfDate);
                yield AllocationMapper.toResponseFromAccountType(allocations, totalValue, asOfDate);
            }
            case BY_CURRENCY -> {
                Map<ValidatedCurrency, Money> allocations = assetAllocationService
                    .calculateAllocationByCurrency(portfolio, asOfDate);
                yield AllocationMapper.toResponseFromCurrency(allocations, totalValue, asOfDate);
            }
        };
    }

    /**
     * Get paginated transaction history with optional filters.
     * 
     * This is for user-facing transaction lists, so it returns
     * paginated results rather than all transactions.
     * 
     * @param query Contains userId, optional filters (accountId, type, dates), and pagination
     * @return Paginated transaction history
     */
    public TransactionHistoryView getTransactionHistory(GetTransactionHistoryQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        // Convert Instant to LocalDateTime for criteria
        LocalDateTime startDate = query.startDate() != null 
            ? LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC) 
            : null;
        LocalDateTime endDate = query.endDate() != null 
            ? LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC) 
            : null;
        
        // Build search criteria with optional filters
        TransactionSearchCriteria.TransactionSearchCriteriaBuilder criteriaBuilder = TransactionSearchCriteria.builder()
            .portfolioId(portfolio.getPortfolioId())
            .transactionType(query.transactionType())
            .startDate(startDate)
            .endDate(endDate);
        
        // Add account filter if specified
        if (query.accountId() != null) {
            criteriaBuilder.accountId(query.accountId());
        }
        
        TransactionSearchCriteria criteria = criteriaBuilder.build();
        
        // Query with pagination (convert from 1-based to 0-based page number)
        // Note: TransactionQueryService already sorts by transactionDate DESC by default
        Page<Transaction> transactionPage = transactionQueryService.queryTransactions(
            criteria,
            query.pageNumber() - 1,  // Convert to 0-based index
            query.pageSize()
        );
        
        // Map to response DTOs
        List<TransactionView> transactionResponses = TransactionMapper.toResponseList(
            transactionPage.getContent()
        );

        // Format date range for response
        String dateRange = query.startDate() != null && query.endDate() != null
            ? query.startDate() + " to " + query.endDate()
            : "All time";
        
        return new TransactionHistoryView(
            transactionResponses,
            (int) transactionPage.getTotalElements(),
            query.pageNumber(),
            query.pageSize(),
            dateRange
        );
    }

    /**
     * Get summary information for a specific account.
     * 
     * @param query Contains userId and accountId
     * @return Account summary with current values
     */
    public AccountView getAccountSummary(GetAccountSummaryQuery query) {
        Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");
        
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        Account account = portfolio.getAccount(query.accountId());
        
        return portfolioAssembler.toAccountResponse(account, marketDataService);
    }

    /**
     * Get complete portfolio summary with all accounts and holdings.
     * 
     * @param query Contains userId
     * @return Complete portfolio summary
     */
    public PortfolioView getPortfolioSummary(GetPortfolioSummaryQuery query) {
        Objects.requireNonNull(query, "GetPortfolioSummaryQuery cannot be null");
        
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        return portfolioAssembler.toResponse(portfolio, marketDataService);
    }
}