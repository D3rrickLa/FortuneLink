package com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces;

import java.time.LocalDate;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.InvestmentPerformanceSummary;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;

public interface IInvestmentPerformanceService {
    // calculates various performance metrics (gains.loss, returns) for the user's porfolio and individual holdings
    // dependencies - portfolio, account, holding , and transaction

    public InvestmentPerformanceSummary calculatePortfolioPerformance(UUID portfolioId);
    public Percentage calculateTimeWeightedReturn(UUID portfolioId, LocalDate startDate, LocalDate endDate);

}