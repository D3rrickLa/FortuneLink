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
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionMapper;
import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.validators.CommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;

// TODO: we might need a TransactionRepository

@Service // disabled for now, throws error with unit tests
@Transactional
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

    public TransactionResponse recordAssetPurchase(RecordPurchaseCommand command){
        // 1. Validate command
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                "Invalid purchase command", 
                validationResult.errors()
            );
        }
        
        // 2. Fetch asset information from market data service
        // This validates the symbol exists and gets all necessary metadata
        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(command.symbol())
            .orElseThrow(() -> new AssetNotFoundException(
                "Asset not found: " + command.symbol()
            ));
        
        // 3. Load portfolio aggregate
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));
        
        // 4. Find account within portfolio
        Account account = portfolio.getAccount(command.accountId());
        
        // 5. Create AssetIdentifier with complete market data
        // we aren't doing this anymore, we will be calling the assetInfo toIdentifier() class instead
        // this is better because we keep the domain pure while the app layer handles orchestration and data 
        // fetching
        // AssetIdentifier identifier = new MarketIdentifier(
        //     command.symbol(),
        //     null,
        //     assetInfo.getAssetType(),      // STOCK, ETF, CRYPTO, etc.
        //     assetInfo.getName(),           // Full asset name
        //     assetInfo.getCurrency().getSymbol(),       // Asset's native currency
        //     // NYSE, NASDAQ, etc. Technology, Finance, etc.
        //     Map.of("Exchange", assetInfo.getExchange(), "Sector", assetInfo.getSector())     
        // );
        
        // 6. Create transaction entity
        Transaction transaction = new Transaction(
            TransactionId.randomId(),
            TransactionType.BUY,
            assetInfo.toIdentifier(),  // Convert to domain value object
            command.quantity(),
            command.price(),               // User's actual purchase price
            command.fees(),
            command.transactionDate(),
            command.notes()
        );
        
        // 7. Calculate total cost (price * quantity + fees)
        Money totalCost = transaction.calculateTotalCost();
        
        // 8. Business rule: Check if account has sufficient cash
        if (!account.hasSufficientCash(totalCost)) {
            throw new InsufficientFundsException(
                totalCost,
                account.getCashBalance(),
                command.accountId()
            );
        }
        
        // 9. Apply transaction to portfolio (domain logic handles asset updates)
        portfolio.recordTransaction(account.getAccountId(), transaction);
        
        // 10. Persist changes
        portfolioRepository.save(portfolio);
        // transactionRepository.save(transaction);
        
        // 11. Map to response DTO
        return TransactionMapper.toResponse(transaction, assetInfo);
    } 

    public TransactionResponse recordAssetSale(RecordSaleCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                "Invalid sale command",
                validationResult.errors()
            );
        }

        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(command.symbol())
            .orElseThrow(() -> new AssetNotFoundException("Asset nout found: " + command.symbol()));

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        Asset asset = account.getAsset(assetInfo.toIdentifier());

        if (asset.getQuantity().compareTo(command.quantity()) < 0) {
            throw new InsufficientFundsException(command.symbol(), command.quantity(), asset.getQuantity());
        }

        Transaction transaction = new Transaction( // should really make a specific constructor where id can gen internally
            TransactionId.randomId(),
            TransactionType.SELL,
            assetInfo.toIdentifier(),  // Convert to domain value object
            command.quantity(),
            command.price(),               // User's actual purchase price
            command.fees(),
            command.transactionDate(),
            command.notes()    
        );

        portfolio.recordTransaction(account.getAccountId(), transaction);

        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, assetInfo);
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
