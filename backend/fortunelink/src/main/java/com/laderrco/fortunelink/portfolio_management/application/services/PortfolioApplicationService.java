package com.laderrco.fortunelink.portfolio_management.application.services;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.validators.CommandValidator;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;

import lombok.AllArgsConstructor;
import lombok.Data;

// TODO: we might need a TransactionRepository

@Service // disabled for now, throws error with unit tests
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
    private final CommandValidator commandValidator;

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
    public TransactionResponse recordAssetPurchase(RecordPurchaseCommand recordPurchase){
        return null;
    } // TransactionResponse

    public TransactionResponse recordAssetSale(RecordSaleCommand recordSaleCommand) {
        return null;
    }

    public TransactionResponse recordDeposit(RecordDepositCommand recordDepositCommand) {
        return null;
    }

    public TransactionResponse recordWithdrawal(RecordWithdrawalCommand recordWithdrawalCommand) {
        return null;
    }

    public TransactionResponse recordDividendIncome(RecordIncomeCommand recordIncomeCommand) {
        return null;
    }
    
    public TransactionResponse recordFee(RecordFeeCommand recordFeeCommand) {
        return null;
    } 
    
    // for updates we need to do the following
    /*
        - validate -> ensure the updated transaction doens't violate business rules(e.g. can't sell more
            than you have)
        
        - Recalculation -> Need ot recal cost basis, realized gains if updating BUY/SELL transaction
        - Historical Impact - affects subsequent calculations
    */
    public TransactionResponse updateTransation(UpdateTransactionCommand updateTransactionCommand) {
        return null;
    }
    
    /*
        casacde effects -> deleting a BUY might make subsequent sell invalid (sell shares you never bought)
        audit - consider soft dletes idntead of hard
        validation -> check if deleting htis transaction would create inconsistencies    
    */
    public void deleteTransaction(DeleteTransactionCommand deleteTransactionCommand) {

    }
    public AccountResponse addAccount(AddAccountCommand addAccountCommand) {
        return null;
    } // account response

    public void removeAccount(RemoveAccountCommand removeAccountCommand) {

    }

    // create initial portfolio for new user
    public PortfolioResponse createPortfolio(CreatePortfolioCommand createPortfolioCommand) {
        return null;
    }
    
    // this either soft deletes or permanently removes
    // need 'confirmation' to delete all the data
    public void deletePortfolio(UserId userId) {

    }
}
