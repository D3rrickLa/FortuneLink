package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.UnsupportedTransactionTypeException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder
public class Account implements ClassValidation {
    private final AccountId accountId;
    private String name;
    private AccountType accountType;
    private ValidatedCurrency baseCurrency; // the currency this account is opened in
    private Money cashBalance;
    private List<Asset> assets; // for NON-cash assets only, might need to make this a MAP later
    private List<Transaction> transactions;

    private boolean isActive;
    private Instant closedDate;

    private final Instant systemCreationDate;
    private Instant lastSystemInteraction; // for calculating when you last interacted with this asset
    private int version;
    
    public Account(AccountId accountId, String name, AccountType accountType, ValidatedCurrency baseCurrency, Money cashBalance, List<Asset> assets, List<Transaction> transactions, boolean isActive, Instant closedDate, Instant systemCreationDate, Instant lastSystemInteraction, int version) {
        this.accountId = ClassValidation.validateParameter(accountId);
        this.name = ClassValidation.validateParameter(name);
        this.accountType = ClassValidation.validateParameter(accountType);
        this.baseCurrency = ClassValidation.validateParameter(baseCurrency);
        this.cashBalance = ClassValidation.validateParameter(cashBalance);
        this.assets = assets != null ? new ArrayList<>(assets) : new ArrayList<>(); // Collections.emptyList returns an immutable list
        this.transactions = transactions != null ? new ArrayList<>(transactions) : new ArrayList<>();
        this.isActive = isActive;
        this.closedDate = closedDate;
        this.systemCreationDate = ClassValidation.validateParameter(systemCreationDate);
        this.lastSystemInteraction = ClassValidation.validateParameter(lastSystemInteraction);
        this.version = version;
    }

    public Account(AccountId accountId, String name, AccountType accountType, ValidatedCurrency baseCurrency, Money cashBalance, List<Asset> assets, List<Transaction> transactions) {
        this(accountId, name, accountType, baseCurrency, cashBalance, assets, transactions, true, Instant.now(), Instant.now(), Instant.now(), 1);
    }
    
    // generic, account, nothing in it
    public Account(AccountId randomId, String accountName, AccountType accountType, ValidatedCurrency baseCurrency) {
        this(randomId, accountName, accountType, baseCurrency, Money.ZERO(baseCurrency.getCode()),null,null);
    }

    void deposit(Money money) {
        ClassValidation.validateParameter(money);
        
        // deposit amount currency must match 
        if (!money.currency().equals(this.baseCurrency)) {
            throw new IllegalArgumentException("Cannot deposit money with different currency.");
        }
        if (money.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount deposited must be greater than 0.");
        }

        this.cashBalance = this.cashBalance.add(money);
        updateMetadata();
    }

    void withdraw(Money money) {
        ClassValidation.validateParameter(money);

        if (!money.currency().equals(this.baseCurrency)) {
            throw new CurrencyMismatchException("Cannot withdraw money with different currency.");
        }
        if (money.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount withdrawn must be greater than 0.");
        }
        if (this.cashBalance.isLessThan(money)) {
            throw new InsufficientFundsException("Insufficient cash balance");
        }

        this.cashBalance = this.cashBalance.subtract(money);
        updateMetadata();
    }

    void addAsset(Asset asset) { // validates cash available <- think we mean that hte account should have money in it before we add a full asset
        ClassValidation.validateParameter(asset);
        
        boolean alreadyExists = this.assets.stream().anyMatch(a -> a.getAssetIdentifier().getPrimaryId().equals(a.getAssetIdentifier().getPrimaryId()));

        if (alreadyExists) {
            throw new IllegalStateException(String.format("Asset with identifier %s already exists in this account", asset.getAssetIdentifier().getPrimaryId()));
        }

        this.assets.add(asset);
        updateMetadata();
    }

    void removeAsset(AssetId assetId) { // add proceeds to cash <- no idea what i meant to say here
        ClassValidation.validateParameter(assetId);

        boolean removed = this.assets.removeIf(a -> a.getAssetId().equals(assetId));

        if (removed) {
            updateMetadata();
        }
    }

    void recordTransaction(Transaction transaction) {
        ClassValidation.validateParameter(transaction);
        addTransaction(transaction);
        applyTransaction(transaction);
        updateMetadata();
    }

    void removeTransaction(TransactionId transactionId) {
        ClassValidation.validateParameter(transactionId);
        boolean removed = this.transactions.removeIf(t -> t.getTransactionId().equals(transactionId));
        if (removed) {
            recalculateStateAfterChange();
            updateMetadata();
        }
        else {
            // TODO add a method throw here maybe or some sort of response saying hey it didn't work
        }
    }

    void updateTransaction(TransactionId transactionId, Transaction updatedTransaction) {
        ClassValidation.validateParameter(transactionId);
        ClassValidation.validateParameter(updatedTransaction);

        if (!updatedTransaction.getTransactionId().equals(transactionId)) {
            throw new IllegalArgumentException("Cannot change transaction identity");
        }

        Optional<Transaction> existingTransaction = this.transactions.stream()
            .filter(t -> t.getTransactionId().equals(transactionId))
            .findAny();

        if (existingTransaction.isEmpty()) {
            throw new TransactionNotFoundException(String.format("%s cannot be found", transactionId.toString()));
        }

        this.transactions.remove(existingTransaction.get());
        this.transactions.add(updatedTransaction);
        recalculateStateAfterChange();
        updateMetadata();
    }

    public Asset getAsset(AssetIdentifier assetIdentifierId) throws AssetNotFoundException {
        ClassValidation.validateParameter(assetIdentifierId);
        return this.assets.stream()
            .filter(a -> a.getAssetIdentifier().equals(assetIdentifierId))
            .findFirst()
            .orElseThrow(() -> new AssetNotFoundException("Asset cannot be found with identifier, " + assetIdentifierId));
    }

    public Transaction getTransaction(TransactionId transactionId) {
        ClassValidation.validateParameter(transactionId);
        return this.transactions.stream()
            .filter(t -> t.getTransactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException());
    }

    public Money calculateTotalValue(MarketDataService marketDataService) {
        Money assteValue = assets.stream()
            .map(a -> a.calculateCurrentValue(marketDataService.getCurrentPrice(a.getAssetIdentifier())))
            .reduce(Money.ZERO(baseCurrency), Money::add);
        return cashBalance.add(assteValue);
    }

    public boolean hasSufficientCash(Money inputAmount){
        if (inputAmount.amount().compareTo(this.cashBalance.amount()) > 0) {
           return false; 
        }
        return true;
    }

    public void close() {
        if (this.assets.isEmpty() != true) {
            throw new IllegalStateException("Cannot close account with remaining assets");
        }
        this.isActive = false;
        this.closedDate = Instant.now();
    }
    
    private void updateMetadata() {
        version++;
        this.lastSystemInteraction = Instant.now();
    }

    private void recalculateStateAfterChange() {
        // recalc cash, assets, cost basis, etc.
        cashBalance = Money.ZERO(baseCurrency);
        assets.clear();
        // Replay all transactions in order
        transactions.stream()
            .sorted(Comparator.comparing(Transaction::getTransactionDate))
            .forEach(this::applyTransaction);
        
    }

    // business logic fo each transaction type to update the account state
    // interpreting what eahc type means for accoutn state
    // TODO clean up the switch statement, ref code below
    /*
        private void applyTransactionToAccount(Transaction transaction, Account account) {
        switch (transaction.getType()) {
            case BUY:
                handleBuyTransaction(transaction, account);
                break;
            case SELL:
                handleSellTransaction(transaction, account);
                break;
            case DEPOSIT:
                handleDepositTransaction(transaction, account);
                break;
            case WITHDRAWAL:
                handleWithdrawalTransaction(transaction, account);
                break;
            case DIVIDEND:
            case INTEREST:
                handleIncomeTransaction(transaction, account);
                break;
            case FEE:
                handleFeeTransaction(transaction, account);
                break;
            default:
                throw new IllegalArgumentException("Unsupported transaction type: " + transaction.getType());
        }
    }
    
    */
    private void applyTransaction(Transaction transaction) {
        switch (transaction.getTransactionType()) {
            case DEPOSIT:
                this.cashBalance = this.cashBalance.add(transaction.getPricePerUnit());
                break;
                
            case WITHDRAWAL:
                this.cashBalance = this.cashBalance.subtract(transaction.getPricePerUnit());
                break;
                
            case BUY:
                // Reduce cash by purchase amount + fees
                Money totalCost = transaction.calculateTotalCost(); // price * quantity + fees
                this.cashBalance = this.cashBalance.subtract(totalCost);
                
                // Add or update asset holding
                addOrUpdateAssetFromBuy(transaction);
                break;
                
            case SELL:
                // Increase cash by sale proceeds - fees
                Money netProceeds = transaction.calculateNetAmount(); // price * quantity - fees
                this.cashBalance = this.cashBalance.add(netProceeds);
                
                // Reduce or remove asset holding
                reduceAssetFromSell(transaction);
                break;
                
            case DIVIDEND: // we might need something here for the asset because you know... that is how it works
            case INTEREST:
                if (transaction.isDrip()) {
                    /*
                        DRIP: Reinvest dividend into more shares
                        Dividend amount is sued to buy shares at current price 
                        // No cash balance chance
                        
                    */
                   addOrUpdateAssetFromDrip(transaction);
                }
                else{
                    // Non-DRIP: Add income to cash balance
                    this.cashBalance = this.cashBalance.add(transaction.getPricePerUnit());
                }
                break;
                
            case FEE:
                // Deduct fee from cash balance
                this.cashBalance = this.cashBalance.subtract(transaction.getPricePerUnit());
                break;
                
            case TRANSFER_IN:
                // Handle based on what's being transferred
                if (transaction.getAssetIdentifier() == null) {
                    // Cash transfer
                    this.cashBalance = this.cashBalance.add(transaction.getPricePerUnit());
                } else {
                    // Asset transfer
                    addOrUpdateAssetFromTransfer(transaction);
                }
                break;
                
            case TRANSFER_OUT:
                // Handle based on what's being transferred
                if (transaction.getAssetIdentifier() == null) {
                    // Cash transfer
                    this.cashBalance = this.cashBalance.subtract(transaction.getPricePerUnit());
                } else {
                    // Asset transfer
                    reduceAssetFromTransfer(transaction);
                }
                break;
                
            default:
                // Had to create a new TransactionTypr for this called 'OTHER'
                throw new UnsupportedTransactionTypeException(transaction.getTransactionType().toString() + " is not a supported transaction type");
        }
    }

    private void addTransaction(Transaction transaction) { // might need to make this private as we can call recordTransaction, or unless we are saying to just add it 
        ClassValidation.validateParameter(transaction);
        boolean alreadyExists = this.transactions.stream()
            .anyMatch(t -> t.getTransactionId().equals(transaction.getTransactionId()));

        if (alreadyExists) {
            throw new IllegalStateException(String.format("Transaction with id %s already exists in this account", transaction.getTransactionId()));
        }
        this.transactions.add(transaction);
    }

    private void addOrUpdateAssetFromBuy(Transaction transaction) {
        Optional<Asset> existingAsset = this.assets.stream()
            .filter(a -> a.getAssetIdentifier().equals(transaction.getAssetIdentifier()))
            .findFirst();
        
        if (existingAsset.isPresent()) {
            // Update existing holding
            Asset asset = existingAsset.get();
            BigDecimal newQuantity = asset.getQuantity().add(transaction.getQuantity());
            Money newCostBasis = asset.getCostBasis().add(transaction.calculateTotalCost());
            
            asset.addQuantity(newQuantity);
            asset.updateCostBasis(newCostBasis);
        } else {
            // Create new asset
            Asset newAsset = new Asset(
                AssetId.randomId(),
                transaction.getAssetIdentifier(),
                transaction.getQuantity(),
                transaction.calculateTotalCost(),
                transaction.getTransactionDate()
            );
            this.assets.add(newAsset);
        }
    }

    private void addOrUpdateAssetFromDrip(Transaction transaction) {
        Optional<Asset> existingAsset = this.assets.stream()
            .filter(a -> a.getAssetIdentifier().equals(transaction.getAssetIdentifier()))
            .findFirst();

        if (existingAsset.isPresent()) {
            Asset asset = existingAsset.get();

            // For DRIP: dividend amount divided by price per share = shares purchased
            Money dividendAmount = transaction.getDividendAmount(); // or however you store this
            Money pricePerShare = transaction.getPricePerUnit(); // market price at reinvestment
            
            BigDecimal additionalShares = dividendAmount.amount()
                .divide(pricePerShare.amount(), Precision.getMoneyPrecision(), Rounding.MONEY.getMode());

            // Update quantity
            asset.addQuantity(additionalShares);

            // Update cost basis - the dividend amount becomes part of your cost basis
            // because you're "buying" more shares with that dividend
            Money newCostBasis = asset.getCostBasis().add(dividendAmount);
            asset.updateCostBasis(newCostBasis);
        }
        else {
            // This shouldn't happen in practice (can't DRIP without owning the asset)
            // But handle it by creating a new holding
            Asset newAsset = new Asset(
                AssetId.randomId(),
                transaction.getAssetIdentifier(),
                transaction.getQuantity(),
                transaction.getPricePerUnit().multiply(transaction.getQuantity()), 
                transaction.getTransactionDate()
            );
            this.assets.add(newAsset);
        }
    }

    private void reduceAssetFromSell(Transaction transaction) {
        Asset asset = this.assets.stream()
            .filter(a -> a.getAssetIdentifier().equals(transaction.getAssetIdentifier()))
            .findFirst()
            .orElseThrow(() -> new AssetNotFoundException("Asset can not be found with given identifier, " + transaction.getAssetIdentifier().getPrimaryId()));
        
        BigDecimal newQuantity = asset.getQuantity().subtract(transaction.getQuantity());
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // Sold entire position
            // boolean flag or DTO...
            this.assets.remove(asset);
        } 
        else if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Cannot sell more than owned");
        } 
        else {
            // Partial sale - update quantity and proportionally reduce cost basis
            BigDecimal sellRatio = transaction.getQuantity().divide(asset.getQuantity(), RoundingMode.HALF_UP);
            Money costBasisReduction = asset.getCostBasis().multiply(sellRatio);
            
            asset.addQuantity(newQuantity);
            asset.updateCostBasis(asset.getCostBasis().subtract(costBasisReduction));
        }
    }

    private void addOrUpdateAssetFromTransfer(Transaction transaction) {
        // Similar to addOrUpdateAssetFromBuy but might handle cost basis differently
        // depending on your business rules for transfers
        addOrUpdateAssetFromBuy(transaction);
    }

    private void reduceAssetFromTransfer(Transaction transaction) {
        // Similar to reduceAssetFromSell
        reduceAssetFromSell(transaction);
    }
}
