package com.laderrco.fortunelink.portfolio_management.application.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio_management.application.mappers.AllocationMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.PortfolioMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionMapper;
import com.laderrco.fortunelink.portfolio_management.application.queries.AnalyzeAllocationQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewNetWorthQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewPerformanceQuery;
import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.NetWorthResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PerformanceResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionHistoryResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Service
@Transactional(readOnly = true)
public class PortfolioQueryService {
    private final PortfolioRepository portfolioRepository;
    private final TransactionQueryRepository transactionRepository;
    private final MarketDataService marketDataService;
    private final PerformanceCalculationService performanceCalculationService;
    private final AssetAllocationService assetAllocationService;
    private final PortfolioValuationService portfolioValuationService;
    // LiabilityQueryService liabilityQueryService // ACL interface <- for the future when we have this context
    private final PortfolioMapper portfolioMapper;
    private final TransactionMapper transactionMapper;
    private final AllocationMapper allocationMapper;

    public NetWorthResponse getNetWorth(ViewNetWorthQuery query) {
        Objects.nonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        Instant calculationDate = query.asOfDate() != null ? query.asOfDate() : Instant.now();

        Money totalAssets = portfolioValuationService.calculateTotalValue(portfolio, marketDataService, calculationDate);

        // TODO: When Loan Management context is implemented, fetch liabilities 
        // Example: Money totalLiabilities = liabilitiesQueryService.getTotalLiabilities(quer.userId(), portfolio.getPortfolioCurrencyPreference());
        Money totalLiabilities = Money.ZERO(portfolio.getPortfolioCurrencyPreference()); // place holder

        Money netWorth = totalAssets.subtract(totalLiabilities);

        return new NetWorthResponse(totalAssets, totalLiabilities, netWorth, calculationDate, totalAssets.currency());

    }

    public PerformanceResponse getPortfolioPerformance(ViewPerformanceQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));

        List<Transaction> transactions = transactionRepository.findByDateRange(
            portfolio.getPortfolioId(), 
            LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC), 
            LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC), 
            Pageable.unpaged()
        );

        // Calculate perofrmance metrics 
        Percentage totalReturn = performanceCalculationService.calculateTotalReturn(portfolio, marketDataService);
        
        Money realizedGains = performanceCalculationService.calculateRealizedGains(portfolio, transactions);
        
        Money unrealizedGains = performanceCalculationService.calculateUnrealizedGains(portfolio, marketDataService);
        
        Percentage timeWeightedReturn = performanceCalculationService.calculateTimeWeightedReturn(portfolio);

        // Calculate annualized return
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(query.startDate(), query.endDate());
        double years = daysBetween / 365.25;
        Percentage annualizedReturn = totalReturn.annualize(years);
        String period = query.startDate() + " to " + query.endDate();

        return new PerformanceResponse(
            totalReturn,
            annualizedReturn,
            realizedGains,
            unrealizedGains,
            timeWeightedReturn,
            null, // moneyWeightedReturn - to be implemented
            period
        ); 
    }

    public AllocationResponse getAssetAllocation(AnalyzeAllocationQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));

        Instant asOfDate = query.asOfDate() != null ? query.asOfDate() : Instant.now(); // query as of date doesn't need a check 

        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, marketDataService, asOfDate);
        
        return switch (query.allocationType()) {
            case BY_TYPE -> {
                Map<AssetType, Money> allocations = assetAllocationService
                    .calculateAllocationByType(portfolio, marketDataService, asOfDate);
                yield AllocationMapper.toResponseFromAssetType(allocations, totalValue, asOfDate);
            }
            case BY_ACCOUNT -> {
                Map<AccountType, Money> allocations = assetAllocationService
                    .calculateAllocationByAccount(portfolio, marketDataService, asOfDate);
                yield AllocationMapper.toResponseFromAccountType(allocations, totalValue, asOfDate);
            }
            case BY_CURRENCY -> {
                Map<ValidatedCurrency, Money> allocations = assetAllocationService
                    .calculateAllocationByCurrency(portfolio, marketDataService, asOfDate);
                yield AllocationMapper.toResponseFromCurrency(allocations, totalValue, asOfDate);
            }
        };
    }

    public TransactionHistoryResponse getTransactionHistory(GetTransactionHistoryQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        // Prepare filtering parameters
        LocalDateTime startDate = query.startDate() != null 
            ? LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC) 
            : null;
        LocalDateTime endDate = query.endDate() != null 
            ? LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC) 
            : null;
        
        // Create pageable with sorting (Spring uses 0-based indexing)
        Pageable pageable = PageRequest.of(
            query.pageNumber() - 1,
            query.pageSize(),
            Sort.by(Sort.Direction.DESC, "transactionDate")
        );
        
        // Use database-level filtering with proper pagination
        Page<Transaction> transactionPage;
        if (query.accountId() != null) {
            // Filter by account - much simpler now with direct relationship
            transactionPage = transactionRepository.findByAccountIdAndFilters(
                query.accountId(),
                query.transactionType(),
                startDate,
                endDate,
                pageable
            );
        } else {
            // All transactions for the portfolio
            transactionPage = transactionRepository.findByPortfolioIdAndFilters(
                portfolio.getPortfolioId(),
                query.transactionType(),
                startDate,
                endDate,
                pageable
            );
        }
        
        List<TransactionResponse> transactionResponses = TransactionMapper.toResponseList(
            transactionPage.getContent()
        );
        
        String dateRange = query.startDate() != null && query.endDate() != null
            ? query.startDate() + " to " + query.endDate()
            : "All time";
        
        return new TransactionHistoryResponse(
            transactionResponses,
            (int) transactionPage.getTotalElements(),
            query.pageNumber(),
            query.pageSize(),
            dateRange
        );
    }

    public AccountResponse getAccountSummary(GetAccountSummaryQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        Account account = portfolio.getAccount(query.accountId());
        
        return portfolioMapper.toAccountResponse(account, marketDataService);
    }

    public PortfolioResponse getPortfolioSummary(GetPortfolioSummaryQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        return portfolioMapper.toResponse(portfolio, marketDataService);
    }
}