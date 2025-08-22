package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.expression.spel.ast.OperatorInstanceof;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetBoughtEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetSoldEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.PortfolioCreatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TradeExecutionTransactionDetails;

public class Portfolio {
    private final UserId userId;
    private final PortfolioId portfolioId;
    
    private String portfolioName;
    private String portfolioDescription;
    private Money portfolioCashBalance;
    
    private final Map<LiabilityId, Liability> liabilities;
    private final Map<AssetHoldingId, AssetHolding> holdings;
    private final Map<AssetIdentifier, AssetHoldingId> assetIndex;
    private final List<Transaction> transactions;
    private final List<DomainEvent> domainEvents;

    private long version;
    private final Instant createdOn;
    private Instant lastModifiedAt;
    private String lastOperation;
    
    // constructor for reconstitution from persistence 
    private Portfolio(
        UserId userId, 
        PortfolioId portfolioId, 
        String portfolioName, 
        String portfolioDescription,
        Money portfolioCashBalance, 
        Map<LiabilityId, Liability> liabilities,
        Map<AssetHoldingId, AssetHolding> holdings, 
        List<Transaction> transactions,
        long version,
        Instant createdOn,
        Instant lastModifiedAt,
        String lastOperation
    
    ) { 
        this.userId = Objects.requireNonNull(userId, "User id cannot be null.");
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        this.portfolioName = validatePortfolioName(portfolioName);
        this.portfolioDescription = portfolioDescription.trim();
        this.portfolioCashBalance = Objects.requireNonNull(portfolioCashBalance, "Portfolio cash balance cannot be null.");
        this.liabilities = Objects.requireNonNull(liabilities, "Liabilities cannot be null.");
        this.holdings = Objects.requireNonNull(holdings, "Asset holdings cannot be null.");
        this.transactions = Objects.requireNonNull(transactions, "Transactions cannot be null.");
        
        this.version = version;
        this.createdOn = createdOn;
        this.lastModifiedAt = lastModifiedAt;
        this.lastOperation = lastOperation;

        this.assetIndex = new HashMap<>();
        this.domainEvents = new ArrayList<>();

        addDomainEvent(new PortfolioCreatedEvent(this.portfolioId, userId, portfolioCashBalance, lastModifiedAt));
    }

    // for new portfolio
    public Portfolio (
        UserId userId,
        String portfolioName,
        String portfolioDescription,
        Money initalBalance,
        Instant createdOn
    ) {
        this(
            userId,
            PortfolioId.createRandom(),
            portfolioName,
            portfolioDescription,
            initalBalance,
            new HashMap<>(),
            new HashMap<>(),
            new ArrayList<>(),
            0L,
            createdOn,
            Instant.now(),
            "Portfolio created"
        );
    }

    private String validatePortfolioName(String name) {
        name = Objects.requireNonNull(name, "Portfolio name cannot be null.");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be empty.");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("Portfolio name cannot exceed 100 characters.");
        }

        return name.trim();
    }

    private void validateTransactionDate(Instant transactionDate) {
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        if (transactionDate.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future.");            
        }
    }

    private void validatePrice(Money pricePerUnit) {
        pricePerUnit = Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
        if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per unit must be positive.");
        }

        if (pricePerUnit.amount().compareTo(BigDecimal.valueOf(0.0001)) < 0) {
            throw new IllegalArgumentException("Price too small. minimum price must be 0.0001 or greater.");
        }
    }

    private void validateQuantity(BigDecimal quantity) {
        quantity = Objects.requireNonNull(quantity, "Quantity cannot be null.");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuantityException("Quantity must be positive.");
        }
    }

    private void validateAssetPriceCurrency(AssetIdentifier asset, Money price) {
        if (!asset.assetTradedIn().equals(price.currency())) {
            // this isn't really an error, can turn into one,
            // price might be legitmately converted, but log for now
            System.out.println("now the same");
        }
    }

    private void validateSufficientFunds(Money requiredAmount, String operations) {
        requiredAmount = Objects.requireNonNull(requiredAmount, "Required amount cannot be null.");
        operations = Objects.requireNonNull(operations, "Operation cannot be null.");
        if (this.portfolioCashBalance.add(requiredAmount).isNegative()) {
            throw new InsufficientFundsException(
                String.format("Insufficient cash for %s. Required: %s, Available: %s", operations, requiredAmount.negate(), this.portfolioCashBalance)
            );
        }
    }

    private void validateBaseTransactionDetails(TransactionSource source, String description, List<Fee> fees) {
        source = Objects.requireNonNull(source, "Source cannot be null.");
        fees = Objects.requireNonNull(fees, "Fees cannot be null.");
    }

    private Optional<AssetHolding> findAssetHolding(AssetIdentifier assetIdentifier) {
        assetIdentifier = Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        AssetHoldingId holdingId = this.assetIndex.get(assetIdentifier);
        if (holdingId == null) {
            return Optional.empty();
        }

        AssetHolding holding = holdings.get(holdingId);
        return holding != null && holding.isActive() ? Optional.of(holding) : Optional.empty();
    }

    private AssetHolding createNewAssetHolding(AssetIdentifier assetIdentifier) {
        assetIdentifier = Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        AssetHolding newHolding = new AssetHolding(
            AssetHoldingId.createRandom(), 
            this.portfolioId, 
            assetIdentifier, 
            BigDecimal.ZERO, 
            Money.ZERO(assetIdentifier.assetTradedIn()), // ACB in native currency
            Instant.now()
        );

        this.holdings.put(newHolding.getAssetHoldingId(), newHolding);
        this.assetIndex.put(assetIdentifier, newHolding.getAssetHoldingId());
        return newHolding;
    }

    private void rebuildAssetIndex() {
        this.assetIndex.clear();;
        for (AssetHolding holding : this.holdings.values()) {
            if (holding.isActive()) {
                this.assetIndex.put(holding.getAssetIdentifier(), holding.getAssetHoldingId());
            }
        }
    }

    private void incrementVersion() {
        this.version++;
        this.lastModifiedAt = Instant.now();
    }

    private void recordLastOperation(String operation) {
        this.lastOperation = operation;
        incrementVersion();
    }

    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }


    // --- CORE BUSINESS OPERATIONS - PURE DOMAIN LOGIC ONLY ---

    // so domain logic, we don't have ANY DEPENDENCIES IN HERE< SO ALL CONVERSIONS MUST BE PASSED IN from service
    // enforce business rules and invariants
    // maintain consistency of the aggregate
    // react to business events
    public void buyAsset(
        AssetIdentifier assetIdentifier,
        BigDecimal quantityToBuy,
        Money pricePerUnitInAssetCurrency,
        Money assetCostInPortfolioCurrency, // conversion service gets 
        Money totalFeesInPortfolioCurrency,
        Money totalFeesInAssetCurrency,
        Instant transactionDate,

        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
        AssetIdentifier validateAssetIdentifier = Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        quantityToBuy = Objects.requireNonNull(quantityToBuy, "Quantity cannot be null.");
        pricePerUnitInAssetCurrency = Objects.requireNonNull(pricePerUnitInAssetCurrency, "Price per unit in asset currency cannot be null.");
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        assetCostInPortfolioCurrency = Objects.requireNonNull(assetCostInPortfolioCurrency, "Asset cost in portfolio cannot be null.");
        totalFeesInPortfolioCurrency = Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees in portfolio currency cannot be null.");
        totalFeesInAssetCurrency = Objects.requireNonNull(totalFeesInAssetCurrency, "Total fees in asset currency cannot be null.");

        validateBaseTransactionDetails(source, description, fees);  
        validateTransactionDate(transactionDate);
        validatePrice(pricePerUnitInAssetCurrency);
        validateQuantity(quantityToBuy);
        validateAssetPriceCurrency(validateAssetIdentifier, pricePerUnitInAssetCurrency);

        
        
        // logic validation 
        
        /*
        * // -- stuff that needs to be calculated, you either calculated it in here, or pass it in the parameter -- // 
        * Money totalFees = calculateTotalFees() // fees in native price, all of them
        * Money assetCost = pricePerUnit * quantity
        * 
        * Money totalCostInAssetCurrency = assetCost.add(totalFees)
        * Money totalCostInPortfolioCurrency = convert()
        * 
        * Money netCashImpact = totalCostPortfolio.negate()
        * 
        * this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact)
        * 
        */
        Money assetCostInAssetCurrency = pricePerUnitInAssetCurrency.multiply(quantityToBuy);
        // then we need the totalFees in AssetCurrency
        Money totalCostInAssetCurrency = assetCostInAssetCurrency.add(totalFeesInAssetCurrency);

        // then we need the totalCost in portfolio, which is assetCost and total Fees, we can't use the conversion so we must pass it in
        Money totalCostInPortfolioCurrency = assetCostInPortfolioCurrency.add(totalFeesInPortfolioCurrency);
        validateSufficientFunds(totalCostInPortfolioCurrency, "buyAsset");

        this.portfolioCashBalance = this.portfolioCashBalance.subtract(totalCostInPortfolioCurrency);

        AssetHolding assetHolding = findAssetHolding(validateAssetIdentifier)
            .orElseGet(() -> createNewAssetHolding(validateAssetIdentifier));

        assetHolding.addToPosition(quantityToBuy, totalCostInAssetCurrency);

        TradeExecutionTransactionDetails details = TradeExecutionTransactionDetails.createBuyDetails(
            assetHolding.getAssetHoldingId(),
            validateAssetIdentifier,
            quantityToBuy,
            pricePerUnitInAssetCurrency,
            assetCostInPortfolioCurrency,
            totalFeesInPortfolioCurrency, 
            
            source,
            description,
            fees
        );

        Transaction transaction = new Transaction(
            TransactionId.createRandom(),
            CorrelationId.createRandom(),
            null,
            this.portfolioId,
            TransactionType.BUY,
            TransactionStatus.COMPLETED,
            details,
            totalCostInPortfolioCurrency.negate(),
            transactionDate,
            Instant.now()
        );

        this.transactions.add(transaction);

        addDomainEvent(new AssetBoughtEvent(
            this.portfolioId,
            assetHolding.getAssetHoldingId(),
            validateAssetIdentifier,
            quantityToBuy,
            totalCostInAssetCurrency,
            transactionDate
        ));

        recordLastOperation("Asset bought: " + assetIdentifier.symbol());      
    }

    public void sellAsset(
        AssetIdentifier assetIdentifier,
        BigDecimal quantityToSell,
        Money pricePerUnitInAssetCurrency,

        Money assetProceedsInPortfolioCurrency, // conversion service gets 
        Money totalFeesInPortfolioCurrency,
        Money totalFeesInAssetCurrency,   
        Money realizedGainLossPortfolioCurrency,

        Instant transactionDate,

        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
        AssetIdentifier validateAssetIdentifier = Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        quantityToSell = Objects.requireNonNull(quantityToSell, "Quantity cannot be null.");
        pricePerUnitInAssetCurrency = Objects.requireNonNull(pricePerUnitInAssetCurrency, "Price per unit in asset currency cannot be null.");
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");

        validateBaseTransactionDetails(source, description, fees);  
        validateTransactionDate(transactionDate);
        validatePrice(pricePerUnitInAssetCurrency);
        validateQuantity(quantityToSell);
        validateAssetPriceCurrency(validateAssetIdentifier, pricePerUnitInAssetCurrency);

        AssetHolding assetHolding = findAssetHolding(validateAssetIdentifier)
            .orElseThrow(() -> new AssetNotFoundException("CAnnot sell asset not held in portfolio."));

            
        Money assetGrossProceedsInAssetCurrency = pricePerUnitInAssetCurrency.multiply(quantityToSell);
        Money assetNetProceedsInAssetCurrency = assetGrossProceedsInAssetCurrency.subtract(totalFeesInAssetCurrency);
            
        Money costBasisAssetCurrency = assetHolding.getAverageACBPerUnit().multiply(quantityToSell);
        Money realizedGainLossAssetCurrency = assetNetProceedsInAssetCurrency.subtract(costBasisAssetCurrency);
            
        Money netProceedsInPortflioCurrency = assetProceedsInPortfolioCurrency.subtract(totalFeesInPortfolioCurrency);
        
        assetHolding.removeFromPosition(quantityToSell);
        this.portfolioCashBalance = this.portfolioCashBalance.add(netProceedsInPortflioCurrency);

        TradeExecutionTransactionDetails details = TradeExecutionTransactionDetails.createSellDetails(
            assetHolding.getAssetHoldingId(),
            validateAssetIdentifier,
            quantityToSell,
            pricePerUnitInAssetCurrency,
            assetProceedsInPortfolioCurrency, // no fees
            totalFeesInPortfolioCurrency,
            realizedGainLossAssetCurrency,
            realizedGainLossPortfolioCurrency,
            assetHolding.getAverageACBPerUnit(),

            source,
            description,
            fees
        );
        
        Transaction transaction = new Transaction(
            TransactionId.createRandom(),
            CorrelationId.createRandom(),
            null,
            this.portfolioId,
            TransactionType.SELL,
            TransactionStatus.COMPLETED,
            details,
            netProceedsInPortflioCurrency,
            transactionDate,
            Instant.now()
        );

        this.transactions.add(transaction);

        addDomainEvent(new AssetSoldEvent(
            this.portfolioId,
            assetHolding.getAssetHoldingId(),
            validateAssetIdentifier,
            quantityToSell,
            pricePerUnitInAssetCurrency.multiply(quantityToSell),
            realizedGainLossAssetCurrency,
            transactionDate
        ));

        this.recordLastOperation("Asset sold: " + validateAssetIdentifier.symbol());
    }
}