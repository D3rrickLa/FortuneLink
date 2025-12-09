package com.laderrco.fortunelink.portfolio_management.application.services;

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

    public NetworthResponse getNetWorth(ViewNetWorthQuery viewNetWorthQuery) {
        return null;
    }

    public PerformanceResponse getPortfolioPerformance(ViewPerformanceQuery viewPerformanceQuery) {
        return null;
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