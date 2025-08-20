package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetBoughtEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetSoldEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.CashflowRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityIncurredEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityPaymentRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityUpdatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.PortfolioCreatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.PortfolioDetailsUpdatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.SnapshotRollbackEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.TransactionReversedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Snapshot;
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

/*
 * <<Entities>>
 * Portfolio ✅
 * AssetHolding ✅
 * Liability ✅
 * Transaction ✅
 * User ✅
 * 
 * <<Value Objects>>
 * <<<ENUMS>>>
 * AssetType ✅
 * CryptoSymbols ✅
 * DecimalPrecision ✅
 * FeeType ✅
 * TransactionSource ✅
 * TransactionStatus ✅
 * TransactionType ✅
 * LiabilityType (new) ✅
 * 
 * <<<OTHER>>>
 * AllocationItem -> most likely a separate domain concern (Goal Management)
 * AssetAllocation -> most likely a separate domain concern (Goal Management)
 * AssetIdentifier ✅
 * CommonTransactionInput (removed, in TransactionDetails)
 * ExchangeRate ✅
 * Fee ✅
 * MarketPrice ✅
 * Money ✅
 * PaymentAllocationResult ✅
 * Percentage ✅
 * TransactionMetadata (removed, in TransactionDetails)
 * All Entitiies Id VOs ✅
 * 
 * <<TransactionDetails>>
 * TransactionDetails ✅
 * --------------------------
 * TradeExecutionTransactionDetails ✅
 * CashflowTransactionDetails ✅
 * IncomeTransactionDetails ✅
 * LiabilityIncurredTransactionDetails ✅
 * LiabilityPaymentTransactionDetails ✅
 * ReversalTransactionDetails ✅ <- new method/transaction
 * 
 * <<Services>>
 * CurrencyConversionService ✅
 * MarketDataService 🟨
 * PortfolioDomainService 🟨 -> for logic that doesn't fit into a single aggregate, might/might not be needed
 * 
 * <<Repositories>>
 * PortfolioRepository ✅🟨
 * UserRepository ✅🟨
 * 
 * <<Events>> allows us to build scalable architecture where a change in one aggregate can trigger a rection elsewhere wihtout the aggregate itself knowing hte details of that reaction
 * AssetBoughtEvent ✅
 * AssetSellEvent ✅
 * DividendReceivedEvent
 * PortfolioCreatedEvent ✅
 * LiabilityPaymentRecordedEvent ✅
 * LiabilityIncurredEvent ✅
 * CashflowRecordedEvent ✅
 */

public class Portfolio {
    /*
     * Track assets 
     * manage cash balance
     * event - dividends, interest, etc.
     * updating general info
     * handle liabilities
     */
    
    private final UserId userId;
    private final PortfolioId portfolioId;
    private String portfolioName;
    private String portfolioDescription;
    private Money portfolioCashBalance;

    private final Map<LiabilityId, Liability> liabilities;
    private final Map<AssetHoldingId, AssetHolding> holdings;
    private final Map<AssetIdentifier, AssetHoldingId> assetIndex;
    private final List<Transaction> transactions;
    private final List<Object> domainEvents;
    private final List<Snapshot> snapshots;
    private final CurrencyConversionService conversionService;
    private final MarketDataService marketDataService;

    private long version;
    private long assetIndexVersion;
    private Instant lastModifiedAt;
    private String lastOperations;


    
    public Portfolio(UserId userId, String portfolioName, String portfolioDescription, Money initialBalance, CurrencyConversionService conversionService, MarketDataService marketDataService) {
        this(
            userId, 
            new PortfolioId(UUID.randomUUID()), 
            portfolioName, 
            portfolioDescription, 
            initialBalance, 
            new HashMap<>(), 
            new HashMap<>(), 
            new HashMap<>(), 
            new ArrayList<>(), 
            new ArrayList<>(),
            new ArrayList<>(),
            conversionService, 
            marketDataService
        );

        this.domainEvents.add(new PortfolioCreatedEvent(this.portfolioId, userId, initialBalance, Instant.now()));
    }

    private Portfolio(
        UserId userId, 
        PortfolioId portfolioId, 
        String portfolioName, 
        String portfolioDescription,
        Money portfolioCashBalance, 
        Map<LiabilityId, Liability> liabilities,
        Map<AssetHoldingId, AssetHolding> holdings, 
        Map<AssetIdentifier, AssetHoldingId> assetIndex,
        List<Transaction> transactions,
        List<Object> domainEvents,
        List<Snapshot> snapshots,
        CurrencyConversionService conversionService,
        MarketDataService marketDataService
    ) {
        this.userId = Objects.requireNonNull(userId, "User id cannot be null");
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        this.portfolioName = validatePortfolioName(portfolioName);
        this.portfolioDescription = portfolioDescription; // Can be null
        this.portfolioCashBalance = Objects.requireNonNull(portfolioCashBalance, "Initial balance cannot be null");
        this.liabilities = Objects.requireNonNull(liabilities, "Liabilities map cannot be null");
        this.holdings = Objects.requireNonNull(holdings, "Holdings map cannot be null");
        this.assetIndex = Objects.requireNonNull(assetIndex, "Asset index map cannot be null");
        this.transactions = Objects.requireNonNull(transactions, "Transactions list cannot be null");
        this.domainEvents = Objects.requireNonNull(domainEvents, "Domain events list cannot be null");
        this.snapshots = Objects.requireNonNull(snapshots, "Snapshots list cannot be null");
        this.conversionService = Objects.requireNonNull(conversionService, "Currency conversion service cannot be null");
        this.marketDataService = Objects.requireNonNull(marketDataService, "Maket data service cannot be null");
        this.version = 0;
        this.assetIndexVersion = 0;
        this.lastModifiedAt = Instant.now();
        this.lastOperations = "Portfolio created";
    }
    
    private String validatePortfolioName(String name) {
        Objects.requireNonNull(name, "Portfolio name cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Portfolio name cannot exceed 100 characters");
        }
        return name.trim();
    }

    private void validateTransactionDate(Instant transactionDate) {
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null");
        if (transactionDate.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }
    }

    private static final int MAX_TRANSACTIONS_PER_DAY = 1000;
    private void validateTransactionLimits() {
        long today = Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond();
        long todaysTransactions = transactions.stream()
            .filter(t -> t.getTransactionDate().truncatedTo(ChronoUnit.DAYS).getEpochSecond() == today)
            .count();

        if (todaysTransactions >= MAX_TRANSACTIONS_PER_DAY) {
            throw new IllegalStateException("Daily transaction limit exceeded");
        }
    }

    private Money convertWithFallback(Money amount, Currency targetCurrency, Instant transactionDate) {
        try {
            return conversionService.convert(amount, targetCurrency, transactionDate);
        } catch (Exception e) {
            return conversionService.convertWithLatestRate(amount, targetCurrency);
        }
    }

    private void incrementVersion() {
        this.version++;
    }

    private void incrementAssetIndexVersion() {
        this.assetIndexVersion++;
    }

    private void recordLastOperations(String operation) {
        this.lastOperations = operation;
        this.lastModifiedAt = Instant.now();
    }

    //for assets we use the asset currency, else we use portfolio currency
    private Money calculateTotalFees(List<Fee> fees, Currency targetCurrency, Instant transactionDate) {
        Objects.requireNonNull(targetCurrency, "Target currency cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        
        if (fees == null || fees.isEmpty()) {
            return Money.ZERO(targetCurrency);
        }

        Money totalFees = Money.ZERO(targetCurrency);

        for (Fee fee : fees) {
            Money feeAmount = fee.amount();
            
            // If the fee's currency is different from the target currency, convert it
            if (!feeAmount.currency().equals(targetCurrency)) {
                feeAmount = convertWithFallback(feeAmount, targetCurrency, transactionDate);
            }
            
            totalFees = totalFees.add(feeAmount);
        }
        return totalFees;
    }

    private Optional<AssetHolding> findAssetHolding(AssetIdentifier assetIdentifier) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        AssetHoldingId holdingId = assetIndex.get(assetIdentifier);
        if (holdingId == null) return Optional.empty();
        AssetHolding holding = holdings.get(holdingId);
        // Check if holding is still active (in case of rollbacks or other operations)
        return holding != null && holding.isActive() ? Optional.of(holding) : Optional.empty();
    }

    private AssetHolding createNewAssetHolding(AssetIdentifier assetIdentifier) {
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        AssetHolding newHolding = new AssetHolding(
            new AssetHoldingId(UUID.randomUUID()), 
            this.portfolioId, 
            assetIdentifier, 
            BigDecimal.ZERO, 
            Money.ZERO(assetIdentifier.assetTradedIn()), 
            Instant.now() 
        );

        this.holdings.put(newHolding.getAssetHoldingId(), newHolding);
        this.assetIndex.put(assetIdentifier, newHolding.getAssetHoldingId());
        incrementAssetIndexVersion();
        return newHolding;
    }
    
    private void validateSufficientFunds(Money requiredAmount, String operation) {
        Objects.requireNonNull(requiredAmount, "Required amount cannot be null.");
        Objects.requireNonNull(operation, "Operation cannot be null.");
        if (this.portfolioCashBalance.add(requiredAmount).isNegative()) {
            throw new InsufficientFundsException("Insufficient cash for " + operation + ". Required: " + 
                requiredAmount.negate() + ", Available: " + this.portfolioCashBalance);
        }
    }

    private void validatePrice(Money pricePerUnit, String operation) {
        if (pricePerUnit.amount().compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new IllegalArgumentException("Price per unit must be greater than 0.01");
        }
    }

    // note we are expecting the user is buying in native currency, no conversion, do it on your own
    public void buyAsset(
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit,
        List<Fee> nativeFees,
        Instant transactionDate,
        TransactionSource source,
        String description
    ) {
        // Validation
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null");
        Objects.requireNonNull(source, "Transaction source cannot be null");
        validateTransactionDate(transactionDate);
        validatePrice(pricePerUnit, "asset purchase");
        validateTransactionLimits();

        nativeFees = nativeFees == null ? Collections.emptyList() : nativeFees;
        description = description.trim();
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuantityException("Quantity must be positive");
        }

        if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per unit must be positive");
        }

        Money totalFees = calculateTotalFees(nativeFees, pricePerUnit.currency(), transactionDate);
        Money assetCost = pricePerUnit.multiply(quantity);

        // Now, add them together in the same currency
        // note: if fees are in different currency could be a problem, but that shouldn't be a problem with the totalFees method assignment
        Money totalCostInAssetCurrency = assetCost.add(totalFees);
        Money totalCostPortfolioCurrency = convertWithFallback(totalCostInAssetCurrency, this.portfolioCashBalance.currency(), transactionDate);

        Money netCashImpact = totalCostPortfolioCurrency.negate();
        
        validateSufficientFunds(netCashImpact, "asset purchase");
        
        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);

        AssetHolding assetHolding = findAssetHolding(assetIdentifier)
            .orElseGet(() -> createNewAssetHolding(assetIdentifier));

        assetHolding.addToPosition(quantity, totalCostInAssetCurrency);

        TradeExecutionTransactionDetails details = new TradeExecutionTransactionDetails(
            assetIdentifier, 
            quantity, 
            pricePerUnit, 
            source, 
            description, 
            nativeFees,
            this.portfolioCashBalance.currency(), 
            assetHolding.getAssetHoldingId(), 
            this.conversionService
        );

        Transaction transaction = new Transaction(
            new TransactionId(UUID.randomUUID()), 
            new CorrelationId(UUID.randomUUID()), 
            null, 
            this.portfolioId, 
            TransactionType.BUY, 
            TransactionStatus.COMPLETED, 
            details, 
            netCashImpact, // Fixed: was pricePerUnit
            transactionDate, 
            Instant.now()
        );

        this.transactions.add(transaction);

        AssetBoughtEvent event = new AssetBoughtEvent(
            this.portfolioId,
            assetHolding.getAssetHoldingId(),
            assetIdentifier,
            quantity,
            totalCostInAssetCurrency,
            transactionDate // Fixed: was Instant.now()
        );
        this.domainEvents.add(event);
        incrementVersion();
        recordLastOperations("Asset bought");
    }

    public void sellAsset(
        AssetIdentifier assetIdentifier,
        BigDecimal quantityToSell,
        Money pricePerUnit, 
        List<Fee> nativeFees,
        Instant transactionDate,
        TransactionSource source,
        String description
    ) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null");
        Objects.requireNonNull(quantityToSell, "Quantity to sell cannot be null");
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null");
        Objects.requireNonNull(source, "Transaction source cannot be null");
        validateTransactionDate(transactionDate);
        validatePrice(pricePerUnit, "asset sale");
        validateTransactionLimits();

        nativeFees = nativeFees == null ? Collections.emptyList() : nativeFees;
        description = description.trim();
        
        AssetHolding assetHolding = findAssetHolding(assetIdentifier)
            .orElseThrow(() -> new AssetNotFoundException("Cannot sell asset not held in portfolio."));
        
        
        if (quantityToSell.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuantityException("Quantity to sell must be positive");
        }

        if (assetHolding.getQuantity().compareTo(quantityToSell) < 0) {
            throw new InvalidQuantityException("Cannot sell more units than you have. Available: " + 
                assetHolding.getQuantity() + ", Requested: " + quantityToSell);
        }

        Money totalFees = calculateTotalFees(nativeFees, pricePerUnit.currency(), transactionDate);
        Money grossProceeds = pricePerUnit.multiply(quantityToSell);
        Money netProceedsAssetCurrency = grossProceeds.subtract(totalFees);

        Money costBasisAssetCurrency = assetHolding.getAverageACBPerUnit().multiply(quantityToSell);
        Money realizedGainLossAssetCurrency = netProceedsAssetCurrency.subtract(costBasisAssetCurrency);

        Money netCashImpactPortfolioCurrency = convertWithFallback(netProceedsAssetCurrency, this.portfolioCashBalance.currency(), transactionDate);
        Money realizedGainLossPortfolioCurrency = convertWithFallback(realizedGainLossAssetCurrency, this.portfolioCashBalance.currency(), transactionDate);

        assetHolding.removeFromPosition(quantityToSell);

        if (assetHolding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            // this.holdings.remove(assetHolding.getAssetHoldingId());
            assetHolding.markAsInactive(); // we don't remove the asset holding, we just mark it as inactive
            assetIndex.remove(assetIdentifier); // Remove from index when inactive
            incrementAssetIndexVersion();
        }

        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpactPortfolioCurrency);

        TradeExecutionTransactionDetails details = new TradeExecutionTransactionDetails(
            assetIdentifier,
            quantityToSell.negate(), 
            pricePerUnit,
            source,
            description,
            nativeFees,
            this.portfolioCashBalance.currency(),
            assetHolding.getAssetHoldingId(),
            this.conversionService,
            realizedGainLossAssetCurrency,
            realizedGainLossPortfolioCurrency,
            assetHolding.getAverageACBPerUnit() // Store ACB per unit at time of sale
        );

        Transaction transaction = new Transaction(
            new TransactionId(UUID.randomUUID()),
            new CorrelationId(UUID.randomUUID()),
            null,
            this.portfolioId,
            TransactionType.SELL,
            TransactionStatus.COMPLETED,
            details,
            netCashImpactPortfolioCurrency,
            realizedGainLossPortfolioCurrency,
            transactionDate,
            Instant.now() 
        );
        this.transactions.add(transaction);

        AssetSoldEvent event = new AssetSoldEvent(
            this.portfolioId,
            assetHolding.getAssetHoldingId(),
            assetIdentifier,
            quantityToSell,
            grossProceeds, // keep in asset currency
            realizedGainLossPortfolioCurrency, // in portfolio currency for aggregation
            transactionDate
        );
        this.domainEvents.add(event);
        incrementVersion();
        recordLastOperations("Asset sold");
    }
    
    public LiabilityId  incurrNewLiability(
        LiabilityDetails liabilityDetails,
        Money liabilityAmount,
        TransactionSource source,
        List<Fee> fees,
        Instant incurrenceDate
    ) {
        Objects.requireNonNull(liabilityDetails, "Liability details cannot be null.");
        Objects.requireNonNull(liabilityAmount, "Initial liability amount cannot be null.");
        Objects.requireNonNull(source, "Transaction source cannot be null.");
        Objects.requireNonNull(incurrenceDate, "Date of new liability cannot be null.");
        validateTransactionDate(incurrenceDate);
        validateTransactionLimits();

        fees = fees == null ? Collections.emptyList() : fees;
        


        if (!liabilityAmount.currency().equals(this.portfolioCashBalance.currency())) {
            throw new IllegalArgumentException(
                String.format("Liability currency %s must match portfolio currency %s", 
                    liabilityAmount.currency(), 
                    this.portfolioCashBalance.currency())
            );
        }
        
        LiabilityId liabilityId = new LiabilityId(UUID.randomUUID());
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

        Money summedFees = calculateTotalFees(fees, this.portfolioCashBalance.currency(), incurrenceDate); // the method actual should exist in TransactionDetails.java 
        Money netCashImpact = details.getPrincipalAmount().subtract(summedFees);
        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);

        // create transaction new and add it
        Transaction transaction = new Transaction(
            new TransactionId(UUID.randomUUID()),
            new CorrelationId(UUID.randomUUID()),
            null, // parent id,
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

        // new LiabilityIncurredEvent
        LiabilityIncurredEvent event = new LiabilityIncurredEvent(
            liabilityId, 
            details.getInterestRate(), 
            liabilityAmount,
            Instant.now()
        );

        domainEvents.add(event);
        incrementVersion();
        recordLastOperations("Liability incurred");
        return liabilityId;
    }

    public void recordLiabilityPayment(
        LiabilityId liabilityId, 
        Money paymentAmount,
        TransactionSource source,
        List<Fee> fees,
        Instant transactionDate

    ) {
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null");
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null");
        Objects.requireNonNull(source, "Transaction source cannot be null");
        validateTransactionDate(transactionDate);  
        validateTransactionLimits();

        fees = fees == null ? Collections.emptyList() : fees;

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
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

        PaymentAllocationResult result = liability.recordPayment(paymentAmount, transactionDate); // Fixed: was Instant.now()
        Money totalFees = calculateTotalFees(fees, this.portfolioCashBalance.currency(), transactionDate);

        // Fixed cash impact calculation
        Money totalCashOutflow = paymentAmount.add(totalFees);
        validateSufficientFunds(totalCashOutflow.negate(), "liability payment");
        
        this.portfolioCashBalance = this.portfolioCashBalance.subtract(totalCashOutflow);

        LiabilityPaymentTransactionDetails details = new LiabilityPaymentTransactionDetails(
            liabilityId, 
            result.principalPaid(),
            result.interestPaid(),
            source, 
            "Liability payment", 
            fees
        );
        
        Transaction transaction = new Transaction(
            new TransactionId(UUID.randomUUID()),
            new CorrelationId(UUID.randomUUID()),
            null,
            this.portfolioId,
            TransactionType.PAYMENT,
            TransactionStatus.COMPLETED,
            details,
            totalCashOutflow.negate(), // Fixed: proper cash impact
            transactionDate,
            Instant.now() 
        );
        
        this.transactions.add(transaction);

        LiabilityPaymentRecordedEvent event = new LiabilityPaymentRecordedEvent(
            liabilityId, 
            paymentAmount, 
            result.principalPaid(), // Fixed: was paymentAmount twice
            Instant.now()
        );
        domainEvents.add(event);
        incrementVersion();
        recordLastOperations("Liability payment recorded");
    }

    public void recordCashflow(
        Money amount, // in portfolio currency
        CashflowType cashflowType,
        TransactionSource source,
        String description,
        List<Fee> fees,
        Instant transactionDate
    ) {
        Objects.requireNonNull(amount, "Cashflow amount cannot be null.");
        Objects.requireNonNull(cashflowType, "Cashflow type cannot be null.");
        Objects.requireNonNull(source, "Source cannot be null.");
        Objects.requireNonNull(description, "Description cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        validateTransactionDate(transactionDate);  
        validateTransactionLimits();

        fees = fees == null ? Collections.emptyList() : fees;

        if (!amount.currency().equals(this.portfolioCashBalance.currency())) {
            throw new IllegalArgumentException("Cashflow amount currency must match portfolio's currency.");
        }

        if (amount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }

        // 2. Calculate fees and net impact
        Money totalFees = calculateTotalFees(fees, this.portfolioCashBalance.currency(), transactionDate); // in portfolio currency
        Money netCashImpact;
        TransactionType type;

        switch (cashflowType) {
            case DEPOSIT:
                netCashImpact = amount.subtract(totalFees);
                type = TransactionType.DEPOSIT;
                break;
            case WITHDRAWAL:
                // For a withdrawal, we debit the amount and the fees
                netCashImpact = amount.add(totalFees).negate();
                type = TransactionType.WITHDRAWAL;
                break;
            case DIVIDEND:
            case INTEREST:
            case RENTAL_INCOME:
                netCashImpact = amount.subtract(totalFees);
                type = TransactionType.INCOME;
                break;
            case INTEREST_EXPENSE:
                netCashImpact = amount.negate();
                type = TransactionType.EXPENSE;
                break;
            case FOREIGN_TAX_WITHHELD:
            case BROKERAGE_FEE:
            case MANAGEMENT_FEE:
                // For direct fees, the 'amount' represents the fee itself
                netCashImpact = amount.negate();
                type = TransactionType.FEE;
                break;
            default:
                throw new IllegalArgumentException("Unsupported cashflow type: " + cashflowType);
        }
        
        // 3. Check for sufficient funds and update cash balance
        if (this.portfolioCashBalance.add(netCashImpact).amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient cash for this transaction.");
        }
        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);

        // 4. Create TransactionDetails and Transaction
        CashflowTransactionDetails details = new CashflowTransactionDetails(
            amount,
            cashflowType,
            source,
            description,
            fees
        );

        Transaction transaction = new Transaction(
            new TransactionId(UUID.randomUUID()),
            new CorrelationId(UUID.randomUUID()),
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

        // 5. Publish a Domain Event
        CashflowRecordedEvent event = new CashflowRecordedEvent(
            this.portfolioId,
            details,
            netCashImpact,
            Instant.now()
        );
        this.domainEvents.add(event);
        incrementVersion();
        recordLastOperations("Cashflow recorded");
    }

    public void recordIncome(
        Money amount, // in portfolio currency
        IncomeType incomeType,
        TransactionSource sourceIdentifier, // e.g., asset symbol or bank account name
        String description,
        List<Fee> fees,
        Instant transactionDate
    ) {
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(incomeType, "Income type cannot be null.");
        Objects.requireNonNull(sourceIdentifier, "Source identifier cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        validateTransactionDate(transactionDate);

        // Map the income type to the correct CashflowType
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

        // The recordCashflow method handles all the core logic
        // for updating the cash balance, creating the transaction,
        // and publishing the domain event.
        recordCashflow(
            amount,
            cashflowType,
            // You can create a specific TransactionSource here if needed
            sourceIdentifier,
            description,
            fees,
            transactionDate
        );
    }

    public void reverseTransaction(
        TransactionId transactionId,
        String reason,
        TransactionSource source,
        String description,
        List<Fee> fees,
        Instant transactionDate
    ) {

        Objects.requireNonNull(transactionId, "Transaction id cannot be null.");
        Objects.requireNonNull(reason, "Reason cannot be null.");
        Objects.requireNonNull(source, "Source cannot be null.");
        Objects.requireNonNull(description, "Description cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        validateTransactionDate(transactionDate);
        incrementVersion();
        recordLastOperations("Transaction reversal");
        validateTransactionLimits();

        fees = fees == null ? Collections.emptyList() : fees;
        Money totalFees = calculateTotalFees(fees, this.portfolioCashBalance.currency(), transactionDate); // in portfolio currency

        Transaction originalTransaction = this.transactions.stream()
            .filter(t -> t.getTransactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (originalTransaction.getType() == TransactionType.REVERSAL) {
            throw new IllegalArgumentException("Cannot reverse a reversal transaction");
        }

        // The reversal amount is the exact opposite of the original transaction's net impact.
        // New fees are handled as a separate cashflow.
        Money reversalAmount = originalTransaction.getTransactionNetImpact().negate();
        
        // Update the cash balance with the reversal amount.
        this.portfolioCashBalance = this.portfolioCashBalance.add(reversalAmount);

        // Reverse specific transaction effects
        switch (originalTransaction.getType()) {
            case BUY:
                TradeExecutionTransactionDetails buyDetails = (TradeExecutionTransactionDetails) Objects.requireNonNull(originalTransaction.getTransactionDetails(), "Buy details cannot be null");
                AssetHolding buyHolding = holdings.get(buyDetails.getAssetHoldingId());
                if (buyHolding != null) {
                    buyHolding.removeFromPosition(buyDetails.getQuantity().abs());
                    
                    if (buyHolding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        buyHolding.markAsInactive();
                        assetIndex.remove(buyDetails.getAssetIdentifier());
                        incrementAssetIndexVersion();
                    }
                }    
                // If null, the holding was already sold off completely, so reversal is just cash adjustment
                break;
            case SELL:
                TradeExecutionTransactionDetails sellDetails = (TradeExecutionTransactionDetails) Objects.requireNonNull(originalTransaction.getTransactionDetails(), "Sell details cannot be null");
                AssetHolding sellHolding = holdings.get(sellDetails.getAssetHoldingId());
                if (sellHolding == null) {
                    sellHolding = createNewAssetHolding(sellDetails.getAssetIdentifier());
                }
                
                // CRITICAL FIX: Use the stored ACB per unit from the transaction details
                Money originalACBPerUnit = sellDetails.getAcbPerUnitAtSale();
                if (originalACBPerUnit == null) {
                    // Fallback: use current ACB if available, otherwise use sell price as approximation
                    // This should not happen with the new constructor, but provides safety
                    originalACBPerUnit = sellHolding.getQuantity().compareTo(BigDecimal.ZERO) > 0 
                        ? sellHolding.getAverageACBPerUnit() 
                        : sellDetails.getPricePerUnit();
                }
                
                Money originalCostBasis = originalACBPerUnit.multiply(sellDetails.getQuantity().abs());
                
                sellHolding.addToPosition(sellDetails.getQuantity().abs(), originalCostBasis);
                
                // Reactivate the holding if it was inactive
                if (!sellHolding.isActive()) {
                    sellHolding.reactivate();
                    assetIndex.put(sellDetails.getAssetIdentifier(), sellHolding.getAssetHoldingId());
                    incrementAssetIndexVersion();
                }
                break;
            case LIABILITY_INCURRENCE:
                LiabilityId liabilityId = ((LiabilityIncurrenceTransactionDetails) Objects.requireNonNull(originalTransaction.getTransactionDetails(), "Liability incurrence details cannot be null")).getLiabilityId();
                liabilities.remove(liabilityId);
                break;
            default:
                // For simple cashflow transactions (DEPOSIT, WITHDRAWAL, etc.), the cash balance update is sufficient.
                break;
        }

        // Create the reversal transaction to maintain a complete history.
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            originalTransaction.getTransactionId(), 
            reason, 
            source,
            description,
            fees
        );

        Transaction reversalTransaction = new Transaction(
            new TransactionId(UUID.randomUUID()),
            originalTransaction.getCorrelationId(),
            originalTransaction.getTransactionId(), // Link to the original transaction
            this.portfolioId,
            TransactionType.REVERSAL,
            TransactionStatus.COMPLETED,
            details,
            reversalAmount,
            transactionDate,
            Instant.now()
        );
        this.transactions.add(reversalTransaction);

        // Handle new fees as a separate cashflow transaction if they exist.
        if (fees != null && !fees.isEmpty()) {
            // Here you would call a method to record the fees.
            // E.g., recordCashflow(feesAmount, CashflowType.REVERSAL_FEE, ..., transactionDate)
            // This keeps the reversal logic clean and auditable.
            recordCashflow(
                totalFees,
                CashflowType.BROKERAGE_FEE,
                source,
                "Reversal fee",
                fees,
                transactionDate
            );
        }

        // Publish a domain event for the reversal.
        TransactionReversedEvent event = new TransactionReversedEvent(
            this.portfolioId,
            originalTransaction.getTransactionId(),
            reversalTransaction.getTransactionId(),
            Instant.now()
        );
        this.domainEvents.add(event);
    }

    public Money calculateTotalValue(Instant valuationDate) {
        Objects.requireNonNull(valuationDate, "Valuation date cannot be null.");

        Money totalValue = this.portfolioCashBalance;
        for (AssetHolding holding : holdings.values()) {
            try {
                Money holdingValue = marketDataService.calculateHoldingValue(holding, valuationDate);
                totalValue = totalValue.add(holdingValue);
            } catch (Exception e) {
                // Log the error and continue, or rethrow based on your needs
                // For DIY tool, maybe just skip the problematic holding and log it
                System.err.println("Failed to calculate value for holding: " + holding.getAssetIdentifier() + ". Error: " + e.getMessage());
                // Or use a default value of zero, or the last known value
            }
        }

        for (Liability liability : liabilities.values()) {
            Money liabilityValue = convertWithFallback(
                liability.getCurrentBalance(),
                this.portfolioCashBalance.currency(),
                valuationDate
            );
            totalValue = totalValue.subtract(liabilityValue);
        }
        
        return totalValue;
    }

    public void accrueInterest(Instant accrualDate) {
        Objects.requireNonNull(accrualDate, "Accrual date cannot be null");
        validateTransactionDate(accrualDate);
        incrementVersion();
        recordLastOperations("Interest accrued");
    
        for (Liability liability : liabilities.values()) {
            Money accruedInterest = liability.accrueInterest(accrualDate);
            if (accruedInterest.amount().compareTo(BigDecimal.ZERO) > 0) {
                // Record as expense/cashflow
                recordCashflow(
                    accruedInterest,
                    CashflowType.INTEREST_EXPENSE,
                    TransactionSource.SYSTEM,
                    "Interest accrual for liability " + liability.getLiabilityId(),
                    Collections.emptyList(),
                    accrualDate
                );
            }
        }
    }

    public void updateLiability(LiabilityId liabilityId, LiabilityDetails newDetails) {
        Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
        Liability existingLiability = Optional.ofNullable(liabilities.get(liabilityId))
            .orElseThrow(() -> new IllegalStateException("Liability with id: " + liabilityId.liabilityId().toString()));
        if (newDetails != null) {
            LiabilityDetails oldDetails = existingLiability.getDetails(); // You'd need this getter
            existingLiability.updateDetails(newDetails);
            
            LiabilityUpdatedEvent event = new LiabilityUpdatedEvent(
                liabilityId,
                oldDetails,
                newDetails,
                Instant.now()
            );
            domainEvents.add(event);
            incrementVersion();
            recordLastOperations("Liability updated");
        }

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
        
        PortfolioDetailsUpdatedEvent event = new PortfolioDetailsUpdatedEvent(
            this.portfolioId,
            oldName,
            oldDescription,
            this.portfolioName,
            this.portfolioDescription,
            Instant.now()
        );
        domainEvents.add(event);
        incrementVersion();
        recordLastOperations("Portfolio details updated");
    }


    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
    private static final int MAX_ROLLING_SNAPSHOTS = 10;

    // this is for transaciton status and failure handling
    public void takeSnapshot(String reason) {
        Objects.requireNonNull(reason, "Reason cannot be null");
        this.snapshots.add(new Snapshot(this.portfolioCashBalance, this.liabilities, this.holdings, reason, this.version, this.assetIndexVersion, false));
    
        // Keep only the last N snapshots (except permanent ones)
        if (snapshots.size() > MAX_ROLLING_SNAPSHOTS) {
            // Remove oldest non-permanent snapshot
            snapshots.removeIf(s -> !s.isPermanent() && snapshots.indexOf(s) < snapshots.size() - MAX_ROLLING_SNAPSHOTS);
        }
    }

    public void clearSnapshots() {
        this.snapshots.clear();
    }

    public void takePermanentSnapshot(String reason) {
        Objects.requireNonNull(reason, "Reason cannot be null");
        this.snapshots.add(new Snapshot(this.portfolioCashBalance, this.liabilities, this.holdings, reason, this.version, this.assetIndexVersion, true));
    }

    public void rollbackToLastSnapshot() {
        if (!this.snapshots.isEmpty()) {
            Snapshot lastSnapshot = this.snapshots.get(this.snapshots.size() - 1);
            this.portfolioCashBalance = lastSnapshot.portfolioCashBalance();
            this.liabilities.clear();
            this.liabilities.putAll(lastSnapshot.liabilities());
            this.holdings.clear();
            this.holdings.putAll(lastSnapshot.holdings());
            
            // Rebuild the asset index
            this.assetIndex.clear();
            for (AssetHolding holding : holdings.values()) {
                if (holding.isActive()) {
                    this.assetIndex.put(holding.getAssetIdentifier(), holding.getAssetHoldingId());
                }
            }

            SnapshotRollbackEvent event = new SnapshotRollbackEvent(
                this.portfolioId,
                lastSnapshot.reason(),
                Instant.now()
            );
            domainEvents.add(event);
            incrementVersion();
            recordLastOperations("Snapshot rollback");
            this.snapshots.remove(lastSnapshot);
        }
    }

    // Validation method for debugging index consistency
    public void validateIndexConsistency() {
        // For debugging purposes
        for (Map.Entry<AssetIdentifier, AssetHoldingId> entry : assetIndex.entrySet()) {
            AssetHolding holding = holdings.get(entry.getValue());
            if (holding == null || !holding.isActive()) {
                throw new IllegalStateException("Index inconsistency detected for asset: " + entry.getKey());
            }
        }
    }

    // --- GETTERS ---
    public UserId getUserId() {
        return userId;
    }


    public PortfolioId getPortfolioId() {
        return portfolioId;
    }


    public String getPortfolioName() {
        return portfolioName;
    }


    public String getPortfolioDescription() {
        return portfolioDescription;
    }


    public Money getPortfolioCashBalance() {
        return portfolioCashBalance;
    }


    public Map<LiabilityId, Liability> getLiabilities() {
        return Collections.unmodifiableMap(liabilities);
    }

    public Map<AssetHoldingId, AssetHolding> getHoldings() {
        return Collections.unmodifiableMap(holdings);
    }

    public Map<AssetIdentifier, AssetHoldingId> getAssetIndex() {
        return Collections.unmodifiableMap(assetIndex);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }

    public CurrencyConversionService getConversionService() {
        return conversionService;
    }

    public long getVersion() {
        return version;
    }

    public long getAssetIndexVersion() {
        return assetIndexVersion;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public String getLastOperations() {
        return lastOperations;
    }
    
}