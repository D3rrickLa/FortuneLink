package com.laderrco.fortunelink.portfolio_management.application.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotFoundException;
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
import com.laderrco.fortunelink.portfolio_management.application.utils.PaginationHelper;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Service
@Transactional(readOnly = true)
public class PortoflioQueryService {
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
        Objects.requireNonNull(query);

        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));

        Instant calculationDate = query.asOfDate() != null ? query.asOfDate() : Instant.now();

        Money totalAssets = calculationDate.equals(Instant.now()) 
            ? portfolioValuationService.calculateTotalValue(portfolio, marketDataService)
            : portfolioValuationService.calculateHistoricalValue(portfolio, marketDataService, calculationDate);

        // TODO: When Loan Management context is implemented, fetch liabilities 
        // Money totalLiabilities = liabilityQueryService.getTotalLiabilities(query.userId(), portoflio.getPortfolioCurrencyPreference();
        Money totalLiabilities = Money.ZERO(portfolio.getPortfolioCurrencyPreference());
        
        Money netWorth = totalAssets.subtract(totalLiabilities);

        return new NetWorthResponse(totalAssets, totalLiabilities, netWorth, calculationDate, totalAssets.currency());
        // Objects.requireNonNull(query);
        // Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
        //     .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        // Money totalAssets = portfolioValuationService.calculateTotalValue(portfolio, marketDataService);

        // Money totalLiabilities = Money.ZERO(portfolio.getPortfolioCurrencyPreference()); // placeholder

        // Money netWorth = totalAssets.subtract(totalLiabilities);

        // Instant asOfDate = query.asOfDate() != null 
        //     ? query.asOfDate()
        //     : Instant.now();
            
        // return new NetWorthResponse(totalAssets, totalLiabilities, netWorth, asOfDate, totalAssets.currency());
    }

    public PerformanceResponse getPortfolioPerformance(ViewPerformanceQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        // Get transactions in date range
        List<Transaction> transactions = transactionRepository.findByDateRange(
            portfolio.getPortfolioId(),
            LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC),
            LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC),
            Pageable.unpaged() // need all for performance, we could simplify this even further since we have start and end dates
        );
        
        // Calculate performance metrics
        Percentage totalReturn = performanceCalculationService.calculateTotalReturn(portfolio, marketDataService);
        
        Money realizedGains = performanceCalculationService.calculateRealizedGains(portfolio,transactions);
        
        Money unrealizedGains = performanceCalculationService.calculateUnrealizedGains(portfolio, marketDataService);
        
        Percentage timeWeightedReturn = performanceCalculationService.calculateTimeWeightedReturn(
            portfolio
        );
        
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

        Map<String, Money> allocations;
        switch (query.allocationType()) {
            case BY_TYPE:
                allocations = assetAllocationService.calculateAllocationByType(portfolio, marketDataService)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey().name(), e -> convertPercentageToMoney(e.getValue(), portfolio)));    
                break;

            case BY_ACCOUNT:
                allocations = assetAllocationService.calculateAllocationByAccount(portfolio, marketDataService)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey().name(), e -> convertPercentageToMoney(e.getValue(), portfolio)));
                    break;
                    
            case BY_CURRENCY:
                allocations = assetAllocationService.calculateAllocationByCurrency(portfolio, marketDataService)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey().getSymbol(), e -> convertPercentageToMoney(e.getValue(), portfolio)));
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported allocation type: " + query.allocationType()); // this shouldn't fire with the current implementation
            }

        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, marketDataService);

        return allocationMapper.toResponse(allocations, totalValue);
    }

    public TransactionHistoryResponse getTransactionHistory(GetTransactionHistoryQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        // Get transactions with filters
        List<Transaction> allTransactions;
        
        if (query.startDate() != null && query.endDate() != null) {
            allTransactions = transactionRepository.findByDateRange(
                portfolio.getPortfolioId(),
                LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC),
                LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC),
                Pageable.unpaged() // TODO: implement pagination properly
            );
        } else {
            allTransactions = transactionRepository.findByPortfolioId(
                portfolio.getPortfolioId(), 
                Pageable.unpaged() // TODO: implement pagination properly
            );
        }
        
        // Get asset identifier primary
        Set<String> accountAssetSymbols = null;
        if (query.accountId() != null) {
            Account account = portfolio.getAccount(query.accountId());
            accountAssetSymbols = account.getAssets().stream()
                .map(e -> e.getAssetIdentifier().getPrimaryId())
                .collect(Collectors.toSet());
        }
        
        // Apply filters
        final Set<String> finalAccountAssetSymbols = accountAssetSymbols;
        List<Transaction> filteredTransactions = allTransactions.stream()
            .filter(t -> query.transactionType() == null || 
                        t.getTransactionType().equals(query.transactionType()))
            .filter(t -> query.accountId() == null || 
                        (finalAccountAssetSymbols != null && 
                        finalAccountAssetSymbols.contains(t.getAssetIdentifier().getPrimaryId())))
            .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
            .collect(Collectors.toList());
        
        // Apply pagination
        int offset = PaginationHelper.calculateOffset(query.pageNumber(), query.pageSize());
        int totalCount = filteredTransactions.size();
        
        List<Transaction> paginatedTransactions = filteredTransactions.stream()
            .skip(offset)
            .limit(query.pageSize())
            .collect(Collectors.toList());
        
        List<TransactionResponse> transactionResponses = transactionMapper.toResponseList(
            paginatedTransactions
        );
        
        String dateRange = query.startDate() != null && query.endDate() != null
            ? query.startDate() + " to " + query.endDate()
            : "All time";
        
        return new TransactionHistoryResponse(
            transactionResponses,
            totalCount,
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
        
        return PortfolioMapper.toAccountResponse(account, marketDataService);
    }

    public PortfolioResponse getPortfolioSummary(GetPortfolioSummaryQuery query) {
        Objects.requireNonNull(query);
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        return portfolioMapper.toResponse(portfolio, marketDataService);
    }

    private Money convertPercentageToMoney(Percentage percentage, Portfolio portfolio) {
        Objects.requireNonNull(percentage);
        Objects.requireNonNull(portfolio);
        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, marketDataService);
        return totalValue.multiply(percentage.toPercentage());
    }
}