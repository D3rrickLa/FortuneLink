package com.laderrco.fortunelink.portfolio_management.application.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.laderrco.fortunelink.portfolio_management.application.mappers.PortfolioMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionMapper;
import com.laderrco.fortunelink.portfolio_management.application.models.TransactionSearchCriteria;
import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.validators.CommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
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
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

/**
 * Application service for portfolio command operations (writes).
 * 
 * This service handles all state-changing operations on the Portfolio aggregate.
 * All operations go through the Portfolio aggregate root to maintain consistency.
 * 
 * For read operations, see PortfolioQueryService.
 * 
 * TODO: Currently assumes 1 portfolio per user for MVP. Multi-portfolio support later.
 * TODO: Transaction edits/deletes are hard deletes with no cascade effect checks.
 */
@Service 
@Transactional
@AllArgsConstructor
public class PortfolioApplicationService {
    // Domain Repository (aggregate root)
    private final PortfolioRepository portfolioRepository;
    
    // Application Services
    private final TransactionQueryService transactionQueryService;
    
    // Domain Services
    private final MarketDataService marketDataService;
    
    // Application Utilities
    private final CommandValidator commandValidator;
    private final PortfolioMapper portfolioMapper;
    
    // ========================================================================
    // TRANSACTION RECORDING COMMANDS
    // ========================================================================
    
    public TransactionResponse recordAssetPurchase(RecordPurchaseCommand command) {
        // 1. Validate command
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid purchase command", validationResult.errors());
        }

        // 2. Fetch asset information from market data service
        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(command.symbol())
            .orElseThrow(() -> new AssetNotFoundException("Asset not found: " + command.symbol()));

        // 3. Load portfolio aggregate
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        // 4. Find account within portfolio
        Account account = portfolio.getAccount(command.accountId());

        // 5. Create transaction entity
        Transaction transaction = new Transaction(
            TransactionId.randomId(),
            account.getAccountId(),
            TransactionType.BUY,
            assetInfo.toIdentifier(),
            command.quantity(),
            command.price(), // actual purchase price
            command.fees(),
            command.transactionDate(),
            command.notes()
        );

        // 6. Calculate total cost
        Money totalCost = transaction.calculateTotalCost();

        // 7. Business rule: Check sufficient cash
        if (!account.hasSufficientCash(totalCost)) {
            throw new InsufficientFundsException(totalCost, account.getCashBalance(), command.accountId());
        }

        // 8. Apply transaction to portfolio (domain logic)
        portfolio.recordTransaction(account.getAccountId(), transaction);

        // 9. Persist aggregate
        portfolioRepository.save(portfolio);

        // 10. Return response
        return TransactionMapper.toResponse(transaction, assetInfo);
    }

    public TransactionResponse recordAssetSale(RecordSaleCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid sale command", validationResult.errors());
        }

        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(command.symbol())
            .orElseThrow(() -> new AssetNotFoundException("Asset not found: " + command.symbol()));

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());
        Asset asset = account.getAsset(assetInfo.toIdentifier());

        if (asset.getQuantity().compareTo(command.quantity()) < 0) {
            throw new InsufficientFundsException(command.symbol(), command.quantity(), asset.getQuantity());
        }

        Transaction transaction = new Transaction( 
            TransactionId.randomId(),
            account.getAccountId(),
            TransactionType.SELL,
            assetInfo.toIdentifier(), // Convert to domain value object
            command.quantity(),
            command.price(), // User's actual purchase price
            command.fees(),
            command.transactionDate(),
            command.notes()
        );

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, assetInfo);
    }

    public TransactionResponse recordDeposit(RecordDepositCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid deposit command", validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        Transaction transaction = new Transaction(
            TransactionId.randomId(),
            account.getAccountId(),
            TransactionType.DEPOSIT,
            new CashIdentifier(command.currency().toString()),
            BigDecimal.ONE,
            command.amount(),
            command.fees(),
            command.transactionDate(),
            command.notes()
        );

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, null);
    }

    public TransactionResponse recordWithdrawal(RecordWithdrawalCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid withdrawal command", validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        Money withdrawalAmount = command.amount(); 

        // check sufficient cash
        if (!account.hasSufficientCash(withdrawalAmount)) {
            throw new InsufficientFundsException(withdrawalAmount, account.getCashBalance(), command.accountId());
        }

        Transaction transaction = new Transaction(
            TransactionId.randomId(),
            account.getAccountId(),
            TransactionType.DEPOSIT,
            new CashIdentifier(command.amount().currency().toString()),
            BigDecimal.ONE,
            withdrawalAmount,
            command.fees(),
            command.transactionDate(),
            command.notes()
        );

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, null);

    }

    public TransactionResponse recordDividendIncome(RecordIncomeCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid income command", validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(command.symbol())
            .orElseThrow(() -> new AssetNotFoundException("Asset not found: " + command.symbol()));

        // For DRIP: quantity represents shares purchased with the dividend
        // For non-DRIP: quantity is 1 (tracking the icnome event)
        BigDecimal quantity = command.isDrip() ? command.sharesRecieved() : BigDecimal.ONE;

        Transaction transaction = new Transaction(
            TransactionId.randomId(),
            account.getAccountId(),
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
            throw new InvalidTransactionException("Invalid fee command", validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        Transaction transaction = new Transaction(
            TransactionId.randomId(),
            account.getAccountId(),
            TransactionType.FEE,
            new CashIdentifier(command.currency().toString()),
            BigDecimal.ONE,
            command.totalAmount(), // total value amount in fees
            command.fees(),
            command.transactionDate(),
            command.notes()
        );

        portfolio.recordTransaction(account.getAccountId(), transaction);
        portfolioRepository.save(portfolio);

        return TransactionMapper.toResponse(transaction, null);

    }

    // ========================================================================
    // TRANSACTION UPDATE/DELETE COMMANDS
    // ========================================================================
    
    /**
     * Update an existing transaction.
     * 
     * Validation includes:
     * - Ensuring updated transaction doesn't violate business rules
     * - For SELL transactions: verify sufficient holdings at that point in time
     * 
     * Note: Currently does NOT recalculate cost basis or realized gains.
     * This is a known limitation for MVP.
     */
    public TransactionResponse updateTransation(UpdateTransactionCommand command) {
        // 1. Validate command
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid update command");
        }

        // 2. Get portfolio
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        // 3. Get all transactions for the account using TransactionQueryService
        TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
            .accountId(command.accountId())
            .build();

        List<Transaction> accountTransactions = transactionQueryService.getAllTransactions(criteria);

        // 4. Find the specific transaction to update
        Transaction existingTransaction = accountTransactions.stream()
            .filter(t -> t.getTransactionId().equals(command.transactionId()))
            .findFirst()
            .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        // 5. Basic validations
        if (command.date().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }

        if (command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (command.price().amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        // 6. CRITICAL validation: For SELL transactions, verify sufficient holdings
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

        // 7. Create updated transaction
        Transaction updatedTransaction = createUpdatedTransaction(existingTransaction, command);

        // 8. Update transaction in portfolio aggregate
        portfolio.updateTransaction(command.accountId(), command.transactionId(), updatedTransaction);

        // 9. Save portfolio
        portfolioRepository.save(portfolio);

        // 10. Return response
        return TransactionMapper.toResponse(updatedTransaction, null);

    }

    /**
     * Delete a transaction.
     * 
     * WARNINGS:
     * - This is a HARD delete (permanent removal)
     * - No cascade effect checks (might make subsequent sells invalid)
     * - Consider soft deletes for audit trail in production
     */
    public void deleteTransaction(DeleteTransactionCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid delete command");
        }

        TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
            .accountId(command.accountId())
            .build();
        
        List<Transaction> accountTransactions = transactionQueryService.getAllTransactions(criteria);

        Transaction transaction = accountTransactions.stream() // Verify transaction exists
            .filter(t -> t.getTransactionId().equals(command.transactionId()))
            .findFirst()
            .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        portfolio.removeTransaction(command.accountId(), transaction.getTransactionId());

        portfolioRepository.save(portfolio);
    }

    // ========================================================================
    // ACCOUNT MANAGEMENT COMMANDS
    // ========================================================================

    public AccountResponse addAccount(AddAccountCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidTransactionException("Invalid add account command", validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        // Create new account
        Account account = Account.createNew(
            AccountId.randomId(),
            command.accountName(),
            command.accountType(),
            command.baseCurrency()
        );

        // Add to portfolio (domain logic checks for duplicates)
        portfolio.addAccount(account);

        // Persist
        portfolioRepository.save(portfolio);

        return portfolioMapper.toAccountResponse(account, null);
    }

    public void removeAccount(RemoveAccountCommand command) {
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        Account account = portfolio.getAccount(command.accountId());

        // Check if account is empty
        if (!account.getAssets().isEmpty()) {
            throw new AccountNotEmptyException(command.accountId(), account.getAssets().size());
        }

        portfolio.removeAccount(command.accountId());
        portfolioRepository.save(portfolio);
    }

    public void correctAssetTicket(CorrectAssetTickerCommand command) {
        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        portfolio.correctAssetTicker(
                command.accountId(),
                command.wrongAssetIdentifier(),
                command.correctAssetIdentifier());

        portfolioRepository.save(portfolio);

    }

    // ========================================================================
    // PORTFOLIO LIFECYCLE COMMANDS
    // ========================================================================
    
    /**
     * Create initial portfolio for a new user.
     * 
     * MVP: One portfolio per user.
     */
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
        Portfolio portfolio = Portfolio.createNew(command.userId(), command.defaultCurrency());

        // add a default account if specified in command
        if (command.createDefaultAccount()) {
            Account defaultAccount = Account.createNew(
                AccountId.randomId(),
                "Default Account",
                AccountType.NON_REGISTERED,
                command.defaultCurrency()
            );
            portfolio.addAccount(defaultAccount);
        }

        // Persist
        Portfolio savePortfolio = portfolioRepository.save(portfolio);

        return portfolioMapper.toResponse(savePortfolio, marketDataService);
    }

    /**
     * Delete a portfolio.
     * 
     * Requires explicit confirmation flag.
     * Portfolio must be empty (no accounts/transactions).
     * 
     * MVP: Hard delete. Consider soft delete for production.
     */
    public void deletePortfolio(DeletePortfolioCommand command) {
        ValidationResult validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidCommandException("Invalid delete portfolio command", validationResult.errors());
        }

        // Ensure deletion flag is set
        if (!command.confirmed()) {
            throw new PortfolioDeletionRequiresConfirmationException(
                "Portfolio deletion requires explicit confirmation"
            );
        }

        Portfolio portfolio = portfolioRepository.findByUserId(command.userId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.userId()));

        // Check if portfolio can be deleted
        if (portfolio.containsAccounts()) {
            throw new PortfolioNotEmptyException(
                "Cannot delete portfolio with existing accounts/transactions. " +
                "Portfolio has " + portfolio.getAccounts().size() + " account(s)"
            );
        }

        // two other options, either soft delete or hard delete, based on the command
        // for now in the MVP, we are hard deleteing everything
        // if (command.softDelete()) {
        //     portfolio.markAsDeleted(LocalDateTime.now()); // don't know if we should add
        //     this.portfolioRepository.save(portfolio);
        // }
        // // Option C: Hard delete (permanent removal)
        // else {
        //   portfolioRepository.delete(portfolio.getPortfolioId());
        // }

        portfolioRepository.delete(portfolio.getPortfolioId());
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================
    
    /**
     * Validates that a SELL transaction has sufficient holdings at that point in time.
     * 
     * This prevents selling shares you don't own by checking holdings
     * chronologically up to the sell date.
     */
    private void validateSellTransaction(AssetIdentifier symbol, BigDecimal sellQuantity, Instant sellDate, List<Transaction> assetTransactions, 
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
            } 
            else if (t.getTransactionType() == TransactionType.SELL) {
                holdings = holdings.subtract(t.getQuantity());
            }
        }

        // Check if we have enough shares to sell
        if (holdings.compareTo(sellQuantity) < 0) {
            throw new IllegalStateException(
                String.format(
                    "Insufficient holdings at %s. Available: %s, Attempting to sell: %s",
                    sellDate,
                    holdings,
                    sellQuantity
                )
            );
        }
    }

    /**
     * Creates an updated transaction from existing transaction and command.
     */
    private Transaction createUpdatedTransaction(Transaction existing, UpdateTransactionCommand command) {
        return new Transaction(
                existing.getTransactionId(), // Keep same ID
                existing.getAccountId(),
                command.type(),
                command.identifier(),
                command.quantity(),
                command.price(),
                command.fee(),
                command.date(),
                command.notes());
    }
}
