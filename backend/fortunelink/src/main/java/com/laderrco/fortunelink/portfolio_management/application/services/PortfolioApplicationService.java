package com.laderrco.fortunelink.portfolio_management.application.services;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;

import lombok.AllArgsConstructor;
import lombok.Data;

// @Service // disabled for now, throws error with unit tests
@AllArgsConstructor
@Data
/*
 * Orchestrates use cases and coordinates between domain services, repositories, and infrastructure
 */
public class PortfolioApplicationService {
    // use case handler
    private final PortfolioRepository portfolioRepository;
    private final PortfolioValuationService portfolioValuationService;
    private final MarketDataService marketDataService;

    /*
     * createPortfolio()
        addAccount()
        recordTransaction()
        updateTransaction()
        deleteTransaction()
        getPortfolioSummary() -> other service
        getTransactionHistory() -> other service
        getPerformanceReport() -> other service
        getAssetAllocation() -> other service
     */

    // TODO: change all the 'void' return types to the proper response classes
    public void recordAssetPurchase(/*RecordPUrchaseCommand */){} // TransactionResponse

    public void recordAssetSale() {}

    public void recordDeposit() {}

    public void recordWithdrawal() {}

    public void recordDividentIncome() {}

    public void recordFee() {} 

    public void addAccount() {} // account response

    public void removeAccount() {}

    public void removeTransaction() {}

    public void updateTransation() {}

    public void createPortfolio() {}

    public void deletePortfolio() {} // need 'confirmation' to delete all the data
}
