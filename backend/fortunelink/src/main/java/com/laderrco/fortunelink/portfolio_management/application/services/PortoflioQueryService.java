package com.laderrco.fortunelink.portfolio_management.application.services;

import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.NetworthResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PerformanceResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionHistoryResponse;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PortoflioQueryService {
    private final PortfolioRepository portfolioRepository;
    // private final TransactionRepository transactionRepository;
    private final MarketDataService marketDataService;
    private final PerformanceCalculationService performanceCalculationService;
    private final AssetAllocationService assetAllocationService;
    private final PortfolioApplicationService portfolioApplicationService;
    
    public NetworthResponse getNetWorth(/*ViewNetWorthQuery */) { return null; }
    public PerformanceResponse getPortfolioPerformance(/*ViewPerformanceQuery*/) { return null; }
    public AllocationResponse getAssetAllocation(/*AnalyzeAllocationQuery*/) { return null; }
    public TransactionHistoryResponse getTransactionHistory(/*GetTransactionHistoryQuery*/) { return null; }
    public AccountResponse getAccountSummary(/*GetAccountSummaryQuery*/) { return null; }
    public PortfolioResponse getPortfolioSummary(/*GetPortfolioSummaryQuery*/) { return null; }
}