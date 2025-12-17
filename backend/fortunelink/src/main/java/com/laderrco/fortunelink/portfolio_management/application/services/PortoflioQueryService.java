package com.laderrco.fortunelink.portfolio_management.application.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
import com.laderrco.fortunelink.portfolio_management.application.responses.NetworthResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PerformanceResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionHistoryResponse;
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
    private final PortfolioMapper portfolioMapper;
    private final TransactionMapper transactionMapper;
    private final AllocationMapper allocationMapper;

    public NetworthResponse getNetWorth(ViewNetWorthQuery query) {
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        Money totalAssets = portfolioValuationService.calculateTotalValue(portfolio, marketDataService);

        // TODO: When Loan Management context is implemented, fetch liabilities 
        Money totalLiabilities = Money.ZERO(portfolio.getPortfolioCurrency()); // placeholder

        Money netWorth = totalAssets.subtract(totalLiabilities);

        Instant asOfDate = query.asOfDate() != null 
            ? query.asOfDate()
            : Instant.now();
            
        return new NetworthResponse(totalAssets, totalLiabilities, netWorth, asOfDate, totalAssets.currency());
    }

    public PerformanceResponse getPortfolioPerformance(ViewPerformanceQuery query) {
        Portfolio portfolio = portfolioRepository.findByUserId(query.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(query.userId()));
        
        // Get transactions in date range
        List<Transaction> transactions = transactionRepository.findByDateRange(
            portfolio.getPortfolioId(),
            LocalDateTime.ofInstant(query.startDate(), ZoneOffset.UTC),
            LocalDateTime.ofInstant(query.endDate(), ZoneOffset.UTC),
            null
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

    public AllocationResponse getAssetAllocation(AnalyzeAllocationQuery analyzeAllocationQuery) {
        return null;
    }

    public TransactionHistoryResponse getTransactionHistory(GetTransactionHistoryQuery transactionHistoryQuery) {
        return null;
    }

    public AccountResponse getAccountSummary(GetAccountSummaryQuery accountSummaryQuery) {
        return null;
    }

    public PortfolioResponse getPortfolioSummary(GetPortfolioSummaryQuery portfolioSummaryQuery) {
        return null;
    }
}