package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Position;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.RealizedGain;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

public class Account {
    private final AccountId accountId;
    private String name;
    private AccountType accountType;
    private Currency baseCurrency;
    private Map<AssetSymbol, Position> positions;
    private List<Transaction> transactions;
    private List<RealizedGain> realizedGains;

    private final Instant createdAt;
    private Instant lastUpdatedAt;

    public Account(AccountId accountId, String name, AccountType accountType, Currency baseCurrency,
            Map<AssetSymbol, Position> positions, List<Transaction> transactions, List<RealizedGain> realizedGains,
            Instant createdAt, Instant lastUpdatedAt) {

        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }
        if (baseCurrency == null) {
            throw new IllegalArgumentException("Base currency cannot be null");
        }

        this.accountId = accountId;
        this.name = name;
        this.accountType = accountType;
        this.baseCurrency = baseCurrency;
        this.transactions = transactions;
        this.realizedGains = realizedGains;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void addTransaction(Transaction tx) {

    }

    public Map<AssetSymbol, Position> calculatePositions() {
        return null;
    }

    public Money calculateTotalValue(Map<AssetSymbol, Price> assets) {
        return null;
    }

    // getter of the transaction + others

    // public Account recordTransaction(Transaction tx) {
    // if (tx.isTrade()) {
    // AssetSymbol symbol = tx.execution().asset();
    // Position current = positions.getOrDefault(
    // symbol,
    // new Position(symbol, getAssetType(tx), baseCurrency, List.of()));

    // Position.ApplyResult result = current.apply(tx);

    // // Handle sale-specific logic
    // if (result instanceof Position.ApplyResult.Sale sale) {
    // realizedGains.add(new RealizedGain(
    // tx.transactionId(),
    // symbol,
    // sale.costBasisRealized(),
    // tx.cashDelta(),
    // sale.realizedGainLoss(),
    // sale.lotsConsumed(),
    // tx.occurredAt()));
    // }

    // Position updated = result.newPosition();

    // if (updated.getTotalQuantity().isZero()) {
    // positions.remove(symbol);
    // } else {
    // positions.put(symbol, updated);
    // }
    // }

    // return this;
    // }
}

// public class Account {
//     private final AccountId accountId;
//     private final Currency baseCurrency;
//     private final Map<AssetSymbol, Position> positions;
//     private final List<RealizedGain> realizedGains;
//     private final AccountType accountType;
    
//     public Account(AccountId accountId, AccountType accountType, Currency baseCurrency) {
//         if (accountId == null) {
//             throw new IllegalArgumentException("Account ID cannot be null");
//         }
//         if (accountType == null) {
//             throw new IllegalArgumentException("Account type cannot be null");
//         }
//         if (baseCurrency == null) {
//             throw new IllegalArgumentException("Base currency cannot be null");
//         }
        
//         this.accountId = accountId;
//         this.accountType = accountType;
//         this.baseCurrency = baseCurrency;
//         this.positions = new HashMap<>();
//         this.realizedGains = new ArrayList<>();
//     }
    
//     /**
//      * Record a transaction and update account state accordingly.
//      * 
//      * @param tx Transaction to record
//      * @return Updated account (for method chaining, though this mutates in place)
//      */
//     public Account recordTransaction(Transaction tx) {
//         // Validate transaction belongs to this account
//         if (!tx.accountId().equals(this.accountId)) {
//             throw new IllegalArgumentException(
//                 String.format("Transaction %s belongs to account %s, not %s",
//                     tx.transactionId(), tx.accountId(), this.accountId)
//             );
//         }
        
//         // Validate currency matches
//         if (!tx.cashDelta().currency().equals(this.baseCurrency)) {
//             throw new IllegalArgumentException(
//                 String.format("Transaction currency %s doesn't match account currency %s",
//                     tx.cashDelta().currency(), this.baseCurrency)
//             );
//         }
        
//         // Process trade transactions (BUY/SELL)
//         if (tx.isTrade()) {
//             AssetSymbol symbol = tx.execution().asset();
            
//             // Get existing position or create new empty one
//             Position current = positions.getOrDefault(
//                 symbol,
//                 Position.empty(symbol, tx.metadata().assetType(), baseCurrency)
//             );
            
//             // Apply transaction to position
//             Position.ApplyResult result = current.apply(tx);
            
//             // Handle different result types
//             switch (result) {
//                 case Position.ApplyResult.Purchase purchase -> {
//                     updatePosition(symbol, purchase.newPosition());
//                 }
//                 case Position.ApplyResult.Sale sale -> {
//                     // Record realized gain
//                     realizedGains.add(new RealizedGain(
//                         tx.transactionId(),
//                         symbol,
//                         sale.costBasisRealized(),
//                         tx.cashDelta(),
//                         sale.realizedGainLoss(),
//                         sale.lotsConsumed(),
//                         tx.occurredAt()
//                     ));
                    
//                     updatePosition(symbol, sale.newPosition());
//                 }
//                 case Position.ApplyResult.NoChange noChange -> {
//                     // Nothing to do
//                 }
//             }
//         }
        
//         // TODO: Handle non-trade transactions (DIVIDEND, INTEREST, DEPOSIT, WITHDRAWAL)
//         // These would update a cash position or be tracked separately
        
//         return this;
//     }
    
//     /**
//      * Update or remove a position based on whether it's empty.
//      */
//     private void updatePosition(AssetSymbol symbol, Position position) {
//         if (position.isEmpty()) {
//             positions.remove(symbol);
//         } else {
//             positions.put(symbol, position);
//         }
//     }
    
//     /**
//      * Get a specific position by symbol.
//      */
//     public Optional<Position> getPosition(AssetSymbol symbol) {
//         return Optional.ofNullable(positions.get(symbol));
//     }
    
//     /**
//      * Get all positions (unmodifiable view).
//      */
//     public Map<AssetSymbol, Position> getPositions() {
//         return Collections.unmodifiableMap(positions);
//     }
    
//     /**
//      * Get all realized gains (unmodifiable view).
//      */
//     public List<RealizedGain> getRealizedGains() {
//         return Collections.unmodifiableList(realizedGains);
//     }
    
//     /**
//      * Calculate total market value of all positions.
//      * 
//      * @param priceProvider Function to get current price for a symbol
//      * @return Total value in account's base currency
//      */
//     public Money getTotalValue(Function<AssetSymbol, Money> priceProvider) {
//         return positions.values().stream()
//             .map(pos -> pos.calculateCurrentValue(priceProvider.apply(pos.assetSymbol())))
//             .reduce(Money.ZERO(baseCurrency), Money::add);
//     }
    
//     /**
//      * Calculate total cost basis across all positions.
//      */
//     public Money getTotalCostBasis() {
//         return positions.values().stream()
//             .map(Position::getTotalCostBasis)
//             .reduce(Money.ZERO(baseCurrency), Money::add);
//     }
    
//     /**
//      * Calculate total unrealized gains across all positions.
//      */
//     public Money getTotalUnrealizedGains(Function<AssetSymbol, Money> priceProvider) {
//         return positions.values().stream()
//             .map(pos -> pos.calculateUnrealizedGain(priceProvider.apply(pos.assetSymbol())))
//             .reduce(Money.ZERO(baseCurrency), Money::add);
//     }
    
//     /**
//      * Calculate total realized gains/losses from all sales.
//      */
//     public Money getTotalRealizedGains() {
//         return realizedGains.stream()
//             .map(RealizedGain::gainLoss)
//             .reduce(Money.ZERO(baseCurrency), Money::add);
//     }
    
//     /**
//      * Calculate total return (realized + unrealized gains).
//      */
//     public Money getTotalReturn(Function<AssetSymbol, Money> priceProvider) {
//         Money unrealized = getTotalUnrealizedGains(priceProvider);
//         Money realized = getTotalRealizedGains();
//         return unrealized.add(realized);
//     }
    
//     // Getters
//     public AccountId getAccountId() {
//         return accountId;
//     }
    
//     public Currency getBaseCurrency() {
//         return baseCurrency;
//     }
    
//     public AccountType getAccountType() {
//         return accountType;
//     }
    
//     /**
//      * Realized gain/loss from a sale transaction.
//      * Immutable record for tax reporting and performance tracking.
//      */
//     public record RealizedGain(
//             TransactionId transactionId,
//             AssetSymbol assetSymbol,
//             Money costBasis,
//             Money proceeds,
//             Money gainLoss,
//             List<TaxLot> lotsConsumed,
//             Instant soldAt) {
        
//         public RealizedGain {
//             if (transactionId == null) {
//                 throw new IllegalArgumentException("Transaction ID cannot be null");
//             }
//             if (assetSymbol == null) {
//                 throw new IllegalArgumentException("Asset symbol cannot be null");
//             }
//             if (costBasis == null) {
//                 throw new IllegalArgumentException("Cost basis cannot be null");
//             }
//             if (proceeds == null) {
//                 throw new IllegalArgumentException("Proceeds cannot be null");
//             }
//             if (gainLoss == null) {
//                 throw new IllegalArgumentException("Gain/loss cannot be null");
//             }
//             if (soldAt == null) {
//                 throw new IllegalArgumentException("Sold at cannot be null");
//             }
            
//             lotsConsumed = lotsConsumed == null ? List.of() : List.copyOf(lotsConsumed);
            
//             // Validate currencies match
//             if (!costBasis.currency().equals(proceeds.currency())) {
//                 throw new IllegalArgumentException(
//                     "Cost basis and proceeds must have same currency");
//             }
//             if (!costBasis.currency().equals(gainLoss.currency())) {
//                 throw new IllegalArgumentException(
//                     "Cost basis and gain/loss must have same currency");
//             }
//         }
        
//         /**
//          * Check if this is a long-term capital gain (held > 365 days).
//          * Uses the earliest lot's acquisition date.
//          */
//         public boolean isLongTerm() {
//             if (lotsConsumed.isEmpty()) {
//                 return false;
//             }
            
//             // For FIFO, the first lot consumed is the earliest acquired
//             TaxLot earliestLot = lotsConsumed.get(0);
//             return earliestLot.isLongTerm(soldAt);
//         }
        
//         /**
//          * Get the holding period in days (based on earliest lot).
//          */
//         public long getHoldingPeriodDays() {
//             if (lotsConsumed.isEmpty()) {
//                 return 0;
//             }
            
//             TaxLot earliestLot = lotsConsumed.get(0);
//             return earliestLot.getHoldingPeriodDays(soldAt);
//         }
//     }
// }
