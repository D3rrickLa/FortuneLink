package com.laderrco.fortunelink.portfolio_management.application.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CorrectAssetTickerCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.AccountNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioAlreadyExistsException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioDeletionRequiresConfirmationException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.application.mappers.PortfolioMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionMapper;
import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.validators.CommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;

// TODO: right now we are using findByUserId -> assumes 1 portoflio per user for MVP, multi-portfolio for layer
// for transaction edits/deletes -> no recalculations, atleast not here, in domain yes there is a method, no cascade effect, and hard delete, also no ocmpatibility checks

@Service // disabled for now, throws error with unit tests
@Transactional
@AllArgsConstructor
@Data
/*
 * Orchestrates use cases and coordinates between domain services, repositories,
 * and infrastructure
 */
public class PortfolioApplicationService {
    // use case handler
    private final PortfolioRepository portfolioRepository;
    private final TransactionQueryRepository transactionQueryRepository; 
    private final PortfolioValuationService portfolioValuationService;
    private final MarketDataService marketDataService;
    private final CommandValidator commandValidator;
    private final PortfolioMapper portfolioMapper;

    /*
     * createPortfolio()
     * addAccount()
     * recordTransaction()
     * updateTransaction()
     * deleteTransaction()
     * getPortfolioSummary() -> other service
     * getTransactionHistory() -> other service
     * getPerformanceReport() -> other service
     * getAssetAllocation() -> other service
     */

    public TransactionResponse recordAssetPurchase(RecordPurchaseCommand command) {
        // 1. Validate command
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                    "Invalid purchase command",
                    validationResult.errors());
        }

        // 2. Fetch asset information from market data service
        // This validates the symbol exists and gets all necessary metadata
        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(command.symbol())
                .orElseThrow(() -> new AssetNotFoundException(
                        "Asset not found: " + command.symbol()));

        // 3. Load portfolio aggregate
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        // 4. Find account within portfolio
        Account account = portfolio.getAccount(command.accountId());

        // 6. Create transaction entity
        Transaction transaction = new Transaction(
                TransactionId.randomId(),
                TransactionType.BUY,
                assetInfo.toIdentifier(), // Convert to domain value object
                command.quantity(),
                command.price(), // User's actual purchase price
                command.fees(),
                command.transactionDate(),
                command.notes());

        // 7. Calculate total cost (price * quantity + fees)
        Money totalCost = transaction.calculateTotalCost();

        // 8. Business rule: Check if account has sufficient cash
        if (!account.hasSufficientCash(totalCost)) {
            throw new InsufficientFundsException(
                    totalCost,
                    account.getCashBalance(),
                    command.accountId());
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
                    validationResult.errors());
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

        Transaction transaction = new Transaction( // should really make a specific constructor where id can gen
                                                   // internally
                TransactionId.randomId(),
                TransactionType.SELL,
                assetInfo.toIdentifier(), // Convert to domain value object
                command.quantity(),
                command.price(), // User's actual purchase price
                command.fees(),
                command.transactionDate(),
                command.notes());

        portfolio.recordTransaction(account.getAccountId(), transaction);

        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, assetInfo);
    }

    public TransactionResponse recordDeposit(RecordDepositCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                    "Invalid deposit command",
                    validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        Transaction transaction = new Transaction(
                TransactionId.randomId(),
                TransactionType.DEPOSIT,
                new CashIdentifier(command.currency().toString()),
                BigDecimal.ONE,
                command.amount(),
                command.fees(),
                command.transactionDate(),
                command.notes());

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, null); // TODO TransactionMapper need to have logic to allow
                                                                // this
    }

    public TransactionResponse recordWithdrawal(RecordWithdrawalCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                    "Invalid withdrawal command",
                    validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        Money withdrawalAmount = new Money(command.amount().amount(), command.currency()); // this is stuipd either
                                                                                           // change it or change the
                                                                                           // command to be BigDecimal
                                                                                           // and not Money

        // check sufficient cash
        if (!account.hasSufficientCash(withdrawalAmount)) {
            throw new InsufficientFundsException(
                    withdrawalAmount,
                    account.getCashBalance(),
                    command.accountId());
        }

        Transaction transaction = new Transaction(
                TransactionId.randomId(),
                TransactionType.DEPOSIT,
                new CashIdentifier(command.currency().toString()),
                BigDecimal.ONE,
                withdrawalAmount,
                command.fees(),
                command.transactionDate(),
                command.notes());

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, null);

    }

    // this is a bit different to handle, because dividend is tied to a stock for the most part
    public TransactionResponse recordDividendIncome(RecordIncomeCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                    "Invalid income command",
                    validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(command.symbol())
                .orElseThrow(() -> new AssetNotFoundException(
                        "Asset not found: " + command.symbol()));

        // For DRIP: quantity represents shares purchased with the dividend
        // For non-DRIP: quantity is 1 (tracking the icnome event)
        BigDecimal quantity = command.isDrip() ? command.sharesRecieved() : BigDecimal.ONE;

        Transaction transaction = new Transaction(
            TransactionId.randomId(),
            TransactionType.DIVIDEND,
            assetInfo.toIdentifier(),
            quantity,
            command.amount().divide(command.sharesRecieved()),
            command.amount(),
            null,
            command.transactionDate(),
            command.notes(),
            command.isDrip()
        );

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, assetInfo);
    }

    public TransactionResponse recordFee(RecordFeeCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                    "Invalid fee command",
                    validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        Transaction transaction = new Transaction(
                TransactionId.randomId(),
                TransactionType.FEE,
                new CashIdentifier(command.currency().toString()),
                BigDecimal.ONE,
                command.totalAmount(), // total value amount in fees
                command.fees(),
                command.transactionDate(),
                command.notes());

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, null);

    }

    // for updates we need to do the following
    /*
     * - validate -> ensure the updated transaction doens't violate business
     * rules(e.g. can't sell more
     * than you have)
     * 
     * - Recalculation -> Need ot recal cost basis, realized gains if updating
     * BUY/SELL transaction
     * - Historical Impact - affects subsequent calculations
     */
    public TransactionResponse updateTransation(UpdateTransactionCommand command) {
        // 1. Validate command
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid update command");
        }
        
        // 2. Get all transactions for the account
        List<Transaction> accountTransactions = transactionQueryRepository
                .findByAccountId(command.accountId(), Pageable.unpaged());
        
        // 3. Find the specific transaction to update
        Transaction existingTransaction = accountTransactions.stream()
                .filter(t -> t.getTransactionId().equals(command.transactionId()))
                .findFirst()
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        
        // // 4. Basic validations
        // if (command.date().isAfter(Instant.now())) {
        //     throw new IllegalArgumentException("Transaction date cannot be in the future");
        // }
        
        // if (command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
        //     throw new IllegalArgumentException("Quantity must be positive");
        // }
        
        // if (command.price().amount().compareTo(BigDecimal.ZERO) <= 0) {
        //     throw new IllegalArgumentException("Price must be positive");
        // }
        
        // 5. ONLY critical validation: can't sell more than you own
        if (command.type() == TransactionType.SELL) {
            List<Transaction> assetTransactions = accountTransactions.stream()
                    .filter(t -> t.getAssetIdentifier().equals(command.identifier()))
                    .sorted((t1, t2) -> t1.getTransactionDate().compareTo(t2.getTransactionDate()))
                    .collect(Collectors.toList());
            
            validateSellTransaction(
                command.identifier(),
                command.quantity(),
                command.date(),
                assetTransactions,
                existingTransaction.getTransactionId()
            );
        }
        
        // 6. Create updated transaction
        Transaction updatedTransaction = createUpdatedTransaction(existingTransaction, command);
        
        // 7. Get portfolio by userId (assume user has only 1 portfolio for MVP)
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));
        
        // 8. Update transaction in portfolio
        portfolio.updateTransaction(command.accountId(), command.transactionId(), updatedTransaction); // we need also the accoutn id and transaction id
        
        // 9. Save portfolio (this saves the updated transaction through aggregate)
        portfolioRepository.save(portfolio);
        
        // 10. Return response
        return TransactionMapper.toResponse(updatedTransaction, null);
        
    }

    /*
     * casacde effects -> deleting a BUY might make subsequent sell invalid (sell
     * shares you never bought)
     * audit - consider soft dletes idntead of hard
     * validation -> check if deleting htis transaction would create inconsistencies
     */
    public void deleteTransaction(DeleteTransactionCommand command) {
        // 1. Validate command
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid delete command");
        }
        
        // 2. Get all transactions for the account
        List<Transaction> accountTransactions = transactionQueryRepository
                .findByAccountId(command.accountId(), Pageable.unpaged());
        
        // 3. Find the transaction to delete
        Transaction transaction = accountTransactions.stream()
                .filter(t -> t.getTransactionId().equals(command.transactionId()))
                .findFirst()
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        
        // 4. Get portfolio
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));
        
        // 5. Remove transaction from portfolio
        portfolio.removeTransaction(command.accountId(), transaction.getTransactionId()); // we need also the accoutn id and transaction id
        
        // 6. Save portfolio
        portfolioRepository.save(portfolio);
    }

    public AccountResponse addAccount(AddAccountCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException(
                    "Invalid add account command",
                    validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        // Create new account
        Account account = new Account(
                AccountId.randomId(),
                command.accountName(),
                command.accountType(),
                command.baseCurrency());

        // Add to portfolio (will check for duplicates)
        portfolio.addAccount(account);

        // Persist
        portfolioRepository.save(portfolio);

        return PortfolioMapper.toAccountResponse(account, null);
    }

    public void removeAccount(RemoveAccountCommand command) {
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        // Check if account is empty
        if (!account.getAssets().isEmpty()) {
            throw new AccountNotEmptyException(
                    command.accountId(),
                    account.getAssets().size());
        }

        portfolio.removeAccount(command.accountId());
        portfolioRepository.save(portfolio);
    }

    @Transactional
    public void correctAssetTicket(CorrectAssetTickerCommand command) {
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));
        
        portfolio.correctAssetTicker(
            command.accountId(),
            command.wrongAssetIdentifier(),
            command.correctAssetIdentifier()
        );
        
        portfolioRepository.save(portfolio);


    }

    // create initial portfolio for new user
    public PortfolioResponse createPortfolio(CreatePortfolioCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidCommandException("Invalid create portfolio command", validationResult.errors());
        }

        // check if portfolio already exists for this user
        Optional<Portfolio> exsitingPortfolio = portfolioRepository.findByUserId(command.userId());
        if (exsitingPortfolio.isPresent()) {
            throw new PortfolioAlreadyExistsException("Portfolio already exists for user: " + command.userId());
        }

        // create portfolio (domain logic)
        Portfolio portfolio = new Portfolio(command.userId(), command.defaultCurrency());

        // add a default account if specified in command
        if (command.createDefaultAccount()) {
            Account defaultAccount = new Account(
                    AccountId.randomId(),
                    "Default Account",
                    AccountType.NON_REGISTERED,
                    command.defaultCurrency());
            portfolio.addAccount(defaultAccount);
        }

        // persist using repo - JPA will happen in the infra layer
        Portfolio savePortfolio = portfolioRepository.save(portfolio);

        return portfolioMapper.toResponse(savePortfolio, marketDataService);
    }

    // this either soft deletes or permanently removes
    // need 'confirmation' to delete all the data
    public void deletePortfolio(DeletePortfolioCommand command) {
        // ensures deletion flag is set
        if (!command.confirmed()) {
            throw new PortfolioDeletionRequiresConfirmationException(
                    "Portfolio deletion requires explicit confirmation");
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        // check if portfolio can be deleted

        // check 1: portfolio must be empty
        if (!portfolio.containsAccounts()) {
            throw new PortfolioNotEmptyException(
                    "Cannot delete portfolio with existing accounts/transactions. " +
                            "Portfolio has " + portfolio.getAccounts().size() + " account(s)");
        }

        // two other options, either soft delete or hard delete, based on the command
        // for now in the MVP, we are hard deleteing everything
        // if (command.softDelete()) {
        //     portfolio.markAsDeleted(LocalDateTime.now()); // don't know if we should add this
        //     portfolioRepository.save(portfolio);
        // }
        // // Option C: Hard delete (permanent removal)
        // else {
        //     portfolioRepository.delete(portfolio.getPortfolioId());
        // }
        portfolioRepository.delete(portfolio.getPortfolioId());
    }
    
    /**
     * Validates that a SELL transaction has sufficient holdings at that point in time.
     */
    private void validateSellTransaction(
            AssetIdentifier symbol,
            BigDecimal sellQuantity,
            Instant sellDate,
            List<Transaction> assetTransactions,
            TransactionId excludeTransactionId) {
        
        BigDecimal holdings = BigDecimal.ZERO;
        
        // Calculate holdings up to the sell date, excluding the transaction being updated
        for (Transaction t : assetTransactions) {
            if (t.getTransactionId().equals(excludeTransactionId)) {
                continue; // Skip the transaction being updated
            }
            
            if (t.getTransactionDate().isAfter(sellDate)) {
                break; // Stop at transactions after the sell date
            }
            
            if (t.getTransactionType() == TransactionType.BUY) {
                holdings = holdings.add(t.getQuantity());
            } else if (t.getTransactionType() == TransactionType.SELL) {
                holdings = holdings.subtract(t.getQuantity());
            }
        }
        
        // Check if we have enough shares to sell
        if (holdings.compareTo(sellQuantity) < 0) {
            throw new IllegalStateException(
                "Insufficient holdings at " + sellDate + ". " +
                "Available: " + holdings + ", Attempting to sell: " + sellQuantity
            );
        }
    }
    
    /**
     * Creates an updated transaction from existing transaction and command.
     */
    private Transaction createUpdatedTransaction(Transaction existing, UpdateTransactionCommand command) {
        return new Transaction(
            existing.getTransactionId(), // Keep same ID
            command.type(),
            command.identifier(),
            command.quantity(),
            command.price(),
            command.fee(),
            command.date(),
            command.notes()
        );
    }
}
