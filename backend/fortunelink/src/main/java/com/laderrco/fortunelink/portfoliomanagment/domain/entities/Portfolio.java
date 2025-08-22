package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetBoughtEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetSoldEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.CashflowRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityIncurredEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityPaymentRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.PortfolioCreatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.PortfolioDetailsUpdatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.TransactionReversedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.IncomeType;
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
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.ReversalTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TradeExecutionTransactionDetails;

public class Portfolio {
    private static final int MAX_PORTFOLIO_NAME_LENGTH = 100;
    private static final BigDecimal MIN_PRICE_THRESHOLD = BigDecimal.valueOf(0.0001);
    
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
        this.portfolioDescription = portfolioDescription != null ? portfolioDescription.trim() : "";
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
        
        // Rebuild the asset index from holdings
        rebuildAssetIndex();
    }

    // for new portfolio
    public Portfolio (
        UserId userId,
        String portfolioName,
        String portfolioDescription,
        Money initialBalance,
        Instant createdOn
    ) {
        this(
            userId,
            PortfolioId.createRandom(),
            portfolioName,
            portfolioDescription,
            initialBalance,
            new HashMap<>(),
            new HashMap<>(),
            new ArrayList<>(),
            0L,
            createdOn,
            Instant.now(),
            "Portfolio created"
        );
        
        // Add domain event only for new portfolios
        addDomainEvent(new PortfolioCreatedEvent(this.portfolioId, userId, initialBalance, this.lastModifiedAt));
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
        // mainly used for price per unit (sold and buy)
        // we should have another one for just money
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
            // price might be legitimately converted, but log for now
            System.out.println("Currency mismatch between asset and price");
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
        this.assetIndex.clear();
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
        validateAssetPriceCurrency(validateAssetIdentifier, pricePerUnitInAssetCurrency); // we technically should trust that app service has already validated

        Money assetCostInAssetCurrency = pricePerUnitInAssetCurrency.multiply(quantityToBuy);
        Money totalCostInAssetCurrency = assetCostInAssetCurrency.add(totalFeesInAssetCurrency);
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
        assetProceedsInPortfolioCurrency = Objects.requireNonNull(assetProceedsInPortfolioCurrency, "Asset proceeds in portfolio currency cannot be null.");
        totalFeesInPortfolioCurrency = Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees in portfolio currency cannot be null.");
        totalFeesInAssetCurrency = Objects.requireNonNull(totalFeesInAssetCurrency, "Total fees in asset currency cannot be null.");
        realizedGainLossPortfolioCurrency = Objects.requireNonNull(realizedGainLossPortfolioCurrency, "Realized gain/loss in portfolio currency cannot be null.");
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");

        validateBaseTransactionDetails(source, description, fees);  
        validateTransactionDate(transactionDate);
        validatePrice(pricePerUnitInAssetCurrency);
        validateQuantity(quantityToSell);
        validateAssetPriceCurrency(validateAssetIdentifier, pricePerUnitInAssetCurrency);

        AssetHolding assetHolding = findAssetHolding(validateAssetIdentifier)
            .orElseThrow(() -> new AssetNotFoundException("Cannot sell asset not held in portfolio."));

        Money assetGrossProceedsInAssetCurrency = pricePerUnitInAssetCurrency.multiply(quantityToSell);
        Money assetNetProceedsInAssetCurrency = assetGrossProceedsInAssetCurrency.subtract(totalFeesInAssetCurrency);
            
        Money costBasisAssetCurrency = assetHolding.getAverageACBPerUnit().multiply(quantityToSell);
        Money realizedGainLossAssetCurrency = assetNetProceedsInAssetCurrency.subtract(costBasisAssetCurrency);
            
        Money netProceedsInPortfolioCurrency = assetProceedsInPortfolioCurrency.subtract(totalFeesInPortfolioCurrency);
        
        assetHolding.removeFromPosition(quantityToSell);
        this.portfolioCashBalance = this.portfolioCashBalance.add(netProceedsInPortfolioCurrency);

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
            netProceedsInPortfolioCurrency,
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
    
    public LiabilityId incurrNewLiability(
        LiabilityDetails liabilityDetails,
        Money liabilityAmount, // in portfolio currency
        Money totalFeesInPortfolioCurrency,
        Instant incurrenceDate,
        TransactionSource source,
        String description,
        List<Fee> fees
    ){
        liabilityDetails = Objects.requireNonNull(liabilityDetails, "Details cannot be null.");
        liabilityAmount = Objects.requireNonNull(liabilityAmount, "Liability amount cannot be null.");
        totalFeesInPortfolioCurrency = Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees in portfolio currency cannot be null.");
        incurrenceDate = Objects.requireNonNull(incurrenceDate, "Incurrence date cannot be null.");

        validateBaseTransactionDetails(source, description, fees);
        validateTransactionDate(incurrenceDate);
        
        if (!liabilityAmount.currency().equals(this.portfolioCashBalance.currency())) {
            throw new IllegalArgumentException(
                String.format("Liability currency %s must match portfolio currency %s", 
                liabilityAmount.currency(), this.portfolioCashBalance.currency())
            );
        }

        LiabilityId liabilityId = LiabilityId.createRandom();
        Liability liability = new Liability(
            liabilityId,
            this.portfolioId,
            liabilityDetails,
            liabilityAmount,
            incurrenceDate
        );

        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
            liabilityId,
            liabilityAmount,
            liabilityDetails.annualInterestRate(),
            source,
            liabilityDetails.description(),
            fees
        );
            
        // Net cash impact is liability amount minus fees
        Money netCashImpact = liabilityAmount.subtract(totalFeesInPortfolioCurrency);
        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);

        Transaction transaction = new Transaction(
            TransactionId.createRandom(),
            CorrelationId.createRandom(),
            null,
            this.portfolioId,
            TransactionType.LIABILITY_INCURRENCE,
            TransactionStatus.COMPLETED,
            details,
            netCashImpact,
            incurrenceDate,
            Instant.now()
        );

        this.transactions.add(transaction);
        this.liabilities.put(liabilityId, liability);

        addDomainEvent(new LiabilityIncurredEvent(liabilityId, details.getInterestRate(), liabilityAmount, incurrenceDate));
        recordLastOperation("New Liability: " + liabilityId);
        
        return liabilityId;
    }

    public void recordLiabilityPayment(
        LiabilityId liabilityId,
        Money paymentAmount, // in portfolio currency
        Money totalFeesInPortfolioCurrency,
        Instant transactionDate,
        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
        liabilityId = Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
        paymentAmount = Objects.requireNonNull(paymentAmount, "Payment amount cannot be null.");
        totalFeesInPortfolioCurrency = Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees in portfolio currency cannot be null.");
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        
        validateBaseTransactionDetails(source, description, fees);
        validateTransactionDate(transactionDate);

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }

        if (!paymentAmount.currency().equals(this.portfolioCashBalance.currency())) {
            throw new IllegalArgumentException(
                String.format("Payment currency %s must match portfolio currency %s", 
                    paymentAmount.currency(), 
                    this.portfolioCashBalance.currency())
            );
        }

        Liability liability = this.liabilities.get(liabilityId);
        if (liability == null) {
            throw new IllegalArgumentException("Liability not found: " + liabilityId);
        }

        PaymentAllocationResult result = liability.recordPayment(paymentAmount, transactionDate);
        Money totalCashOutflow = paymentAmount.add(totalFeesInPortfolioCurrency);

        // Validate sufficient funds - pass negative amount as this is cash outflow
        validateSufficientFunds(totalCashOutflow.negate(), "recordLiabilityPayment");

        this.portfolioCashBalance = this.portfolioCashBalance.subtract(totalCashOutflow);

        LiabilityPaymentTransactionDetails details = new LiabilityPaymentTransactionDetails(
            liabilityId, 
            result.principalPaid(),
            result.interestPaid(),
            source, 
            description, // Use the provided description, not hardcoded
            fees
        );
        
        Transaction transaction = new Transaction(
            TransactionId.createRandom(),
            CorrelationId.createRandom(),
            null,
            this.portfolioId,
            TransactionType.PAYMENT,
            TransactionStatus.COMPLETED,
            details,
            totalCashOutflow.negate(), // Negative because it's cash outflow
            transactionDate,
            Instant.now() 
        );
        
        this.transactions.add(transaction);

        addDomainEvent(new LiabilityPaymentRecordedEvent(
            liabilityId, 
            paymentAmount, 
            result.principalPaid(),
            transactionDate
        ));
        
        recordLastOperation("Liability payment recorded: " + liabilityId);
    }

    public void recordCashflow(
        Money amount, 
        CashflowType cashflowType,
        Money totalFeesInPortfolioCurrency, // additional fees, if any
        Instant transactionDate,

        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
        amount = Objects.requireNonNull(amount, "Amount cannot be null.");
        cashflowType = Objects.requireNonNull(cashflowType, "Cashflow type cannot be null.");
        totalFeesInPortfolioCurrency = Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees in portfolio currency cannot be null.");
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        
        validateBaseTransactionDetails(source, description, fees);
        validateTransactionDate(transactionDate);

        // Validate currency matches portfolio currency
        if (!amount.currency().equals(this.portfolioCashBalance.currency())) {
            throw new IllegalArgumentException(
                String.format("Amount currency %s must match portfolio currency %s", 
                    amount.currency(), this.portfolioCashBalance.currency())
            );
        }

        Money netCashImpact;
        TransactionType type;

        switch (cashflowType) {
            case DEPOSIT:
                // For deposits: receive amount, pay fees
                netCashImpact = amount.subtract(totalFeesInPortfolioCurrency);
                type = TransactionType.DEPOSIT;
                break;
                
            case WITHDRAWAL:
                // For withdrawals: pay amount + fees
                netCashImpact = amount.add(totalFeesInPortfolioCurrency).negate();
                type = TransactionType.WITHDRAWAL;
                break;
                
            case DIVIDEND:
            case INTEREST:
            case RENTAL_INCOME:
                // For income: receive amount, may pay fees
                netCashImpact = amount.subtract(totalFeesInPortfolioCurrency);
                type = TransactionType.INCOME;
                break;
                
            case INTEREST_EXPENSE:
                // For expenses: pay amount + any fees
                netCashImpact = amount.add(totalFeesInPortfolioCurrency).negate();
                type = TransactionType.EXPENSE;
                break;
                
            case FOREIGN_TAX_WITHHELD:
            case BROKERAGE_FEE:
            case MANAGEMENT_FEE:
                // For direct fees: the amount IS the fee, totalFeesInPortfolioCurrency should be additional processing fees
                netCashImpact = amount.add(totalFeesInPortfolioCurrency).negate();
                type = TransactionType.FEE;
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported cashflow type: " + cashflowType);
        }

        // Only validate sufficient funds for outflows (negative amounts)
        if (netCashImpact.isNegative()) {
            validateSufficientFunds(netCashImpact, "recordCashflow");
        }

        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);

        CashflowTransactionDetails details = new CashflowTransactionDetails(
            amount, 
            cashflowType, 
            source, 
            description, 
            fees
        );

        Transaction transaction = new Transaction(
            TransactionId.createRandom(), 
            CorrelationId.createRandom(),
            null, 
            this.portfolioId, 
            type,
            TransactionStatus.COMPLETED, 
            details, 
            netCashImpact, 
            transactionDate, 
            Instant.now()
        );
        
        this.transactions.add(transaction);

        addDomainEvent(new CashflowRecordedEvent(
            this.portfolioId, 
            details, 
            netCashImpact, // Use netCashImpact instead of amount for event
            transactionDate
        ));
        
        recordLastOperation("Cashflow recorded: " + cashflowType + " - " + transaction.getTransactionId());
    }

    public void recordIncome(
        Money amount, 
        IncomeType incomeType,
        Money totalFeesInPortfolioCurrency, // if any
        Instant transactionDate,
        
        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
        amount = Objects.requireNonNull(amount, "Amount cannot be null.");
        incomeType = Objects.requireNonNull(incomeType, "Income type cannot be null.");
        totalFeesInPortfolioCurrency = Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees cannot be null.");
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        
        validateBaseTransactionDetails(source, description, fees);
        validateTransactionDate(transactionDate);

        CashflowType cashflowType;
        switch (incomeType) {
            case DIVIDEND:
                cashflowType = CashflowType.DIVIDEND;
                break;
            case INTEREST:
                cashflowType = CashflowType.INTEREST;
                break;
            case RENTAL:
                cashflowType = CashflowType.RENTAL_INCOME;
                break;
            default:
                throw new IllegalArgumentException("Unsupported income type: " + incomeType);
        }

        recordCashflow(amount, cashflowType, totalFeesInPortfolioCurrency, transactionDate, source, description, fees);
    }

    public void reverseTransaction(
        TransactionId transactionId,
        String reason,
        Money totalFeesInPortfolioCurrency,
        Instant transactionDate,

        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
        TransactionId validatedTransactionId = Objects.requireNonNull(transactionId, "Transaction id cannot be null.");
        reason = Objects.requireNonNull(reason, "Reason cannot be null.");
        transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        
        validateBaseTransactionDetails(source, description, fees);
        validateTransactionDate(transactionDate);
        
        reason = reason.trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty.");
        }

        Transaction originalTransaction = this.transactions.stream()
            .filter(t -> t.getTransactionId().equals(validatedTransactionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + validatedTransactionId));

        if (originalTransaction.getType() == TransactionType.REVERSAL) {
            throw new IllegalArgumentException("Cannot reverse a reversal transaction.");
        }

        Money reversalAmount = originalTransaction.getTransactionNetImpact().negate();

        this.portfolioCashBalance = this.portfolioCashBalance.add(reversalAmount);

        switch (originalTransaction.getType()) {
            case BUY:
                TradeExecutionTransactionDetails buyDetails = (TradeExecutionTransactionDetails) Objects.requireNonNull(originalTransaction.getTransactionDetails(), "Buy details cannot be null");
                AssetHolding buyHolding = holdings.get(buyDetails.getAssetHoldingId());
                if (buyHolding != null) {
                    buyHolding.removeFromPosition(buyDetails.getQuantity().abs());
                    
                    if (buyHolding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        // Note: Assuming these methods exist on AssetHolding
                        buyHolding.markAsInactive();
                        assetIndex.remove(buyDetails.getAssetIdentifier());
                    }
                }    
                break;
                
            case SELL:
                TradeExecutionTransactionDetails sellDetails = (TradeExecutionTransactionDetails) Objects.requireNonNull(originalTransaction.getTransactionDetails(), "Sell details cannot be null");
                AssetHolding sellHolding = holdings.get(sellDetails.getAssetHoldingId());
                if (sellHolding == null) {
                    sellHolding = createNewAssetHolding(sellDetails.getAssetIdentifier());
                }
                
                // Use the stored ACB per unit from the transaction details
                Money originalACBPerUnit = sellDetails.getAcbPerUnitAtSale();
                if (originalACBPerUnit == null) {
                    // Fallback: use current ACB if available, otherwise use sell price as approximation
                    originalACBPerUnit = sellHolding.getQuantity().compareTo(BigDecimal.ZERO) > 0 
                        ? sellHolding.getAverageACBPerUnit() 
                        : sellDetails.getPricePerUnit();
                }
                
                Money originalCostBasis = originalACBPerUnit.multiply(sellDetails.getQuantity().abs());
                sellHolding.addToPosition(sellDetails.getQuantity().abs(), originalCostBasis);
                
                // Reactivate the holding if it was inactive
                if (!sellHolding.isActive()) {
                    // Note: Assuming this method exists on AssetHolding
                    sellHolding.reactivate();
                    assetIndex.put(sellDetails.getAssetIdentifier(), sellHolding.getAssetHoldingId());
                }
                break;
                
            case LIABILITY_INCURRENCE:
                LiabilityIncurrenceTransactionDetails liabilityDetails = (LiabilityIncurrenceTransactionDetails) Objects.requireNonNull(originalTransaction.getTransactionDetails(), "Liability incurrence details cannot be null");
                LiabilityId liabilityId = liabilityDetails.getLiabilityId();
                liabilities.remove(liabilityId);
                break;
                
            default:
                // For simple cashflow transactions (DEPOSIT, WITHDRAWAL, etc.), the cash balance update is sufficient.
                break;
        }

        ReversalTransactionDetails details = new ReversalTransactionDetails(
            originalTransaction.getTransactionId(), 
            reason, 
            source, 
            description, 
            fees
        );

        Transaction reversalTransaction = new Transaction(
            TransactionId.createRandom(),
            originalTransaction.getCorrelationId(),
            originalTransaction.getTransactionId(), // parent
            this.portfolioId,
            TransactionType.REVERSAL,
            TransactionStatus.COMPLETED,
            details,
            reversalAmount,
            transactionDate,
            Instant.now()
        );
        
        // Add the reversal transaction to the list
        this.transactions.add(reversalTransaction);

        // Handle reversal fees as a separate cashflow transaction if they exist
        if (totalFeesInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) > 0) {
            recordCashflow(
                totalFeesInPortfolioCurrency, // The fee amount
                CashflowType.BROKERAGE_FEE,
                Money.ZERO(this.portfolioCashBalance.currency()), // No additional fees on the fee
                transactionDate,
                source,
                "Reversal processing fee",
                fees
            );
        }

        addDomainEvent(new TransactionReversedEvent(
            this.portfolioId, 
            originalTransaction.getTransactionId(), 
            reversalTransaction.getTransactionId(),
            transactionDate
        ));
        
        recordLastOperation("Reversed Transaction: " + originalTransaction.getTransactionId() + " -> " + reversalTransaction.getTransactionId());

    }

    public void updatePortfolioDetails(String newName, String newDescription) {
        String oldName = this.portfolioName;
        String oldDescription = this.portfolioDescription;

        if (newName != null) {
            this.portfolioName = validatePortfolioName(newName);
        }
        if (newDescription != null) {
            this.portfolioDescription = newDescription;
        }

        addDomainEvent(new PortfolioDetailsUpdatedEvent(
            this.portfolioId,
            oldName,
            oldDescription,
            this.portfolioName,
            this.portfolioDescription,
            Instant.now()
        ));

        recordLastOperation("Portfolio details updated");
    }

    // Domain query methods
    public List<AssetHolding> getActiveHoldings() {
        return holdings.values().stream()
            .filter(AssetHolding::isActive)
            .collect(Collectors.toList());
    }

    public Optional<AssetHolding> getHolding(AssetIdentifier assetIdentifier) {
        return findAssetHolding(assetIdentifier);
    }

    public List<Liability> getActiveLiabilities() {
        return new ArrayList<>(liabilities.values());
    }

    // Event management
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void markEventsAsCommitted() {
        domainEvents.clear();
    }


    // Getters
    public UserId getUserId() { return userId; }
    public PortfolioId getPortfolioId() { return portfolioId; }
    public String getPortfolioName() { return portfolioName; }
    public String getPortfolioDescription() { return portfolioDescription; }
    public Money getPortfolioCashBalance() { return portfolioCashBalance; }
    public Currency getPortfolioCurrency() { return portfolioCashBalance.currency(); }
    public Map<AssetHoldingId, AssetHolding> getHoldings() { return Collections.unmodifiableMap(holdings); }
    public Map<LiabilityId, Liability> getLiabilities() { return Collections.unmodifiableMap(liabilities); }
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }
    public long getVersion() { return version; }
    public Instant getCreatedOn() { return createdOn; }
    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public String getLastOperation() { return lastOperation; }
}