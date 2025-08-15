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
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetBoughtEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetSoldEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.CashflowRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityIncurredEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityPaymentRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.PortfolioCreatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.TransactionReversedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
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
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.ReversalTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TradeExecutionTransactionDetails;

/*
 * <<Entities>>
 * Portfolio 🟨
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
 * ReversalTransactionDetails ✅🟨 <- new method/transaction
 * 
 * <<Services>>
 * CurrencyConversionService ✅
 * MarketDataService 🟨
 * PortfolioDomainService 🟨 -> for logic that doesn't fit into a single aggregate, might/might not be needed
 * 
 * <<Repositories>>
 * PortfolioRepository
 * UserRepository
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
    
    private final User user;
    private final PortfolioId portfolioId;
    private String portfolioName;
    private String portfolioDescription;
    private Money portfolioCashBalance;

    private final Map<LiabilityId, Liability> liabilities;
    private final Map<AssetHoldingId, AssetHolding> holdings;
    private final List<Transaction> transactions;
    private final List<Object> domainEvents;
    private final CurrencyConversionService conversionService;

    // need a no arg constructor as it is responsible for init all the internal fields ✅
    // every public method that changes the portfolio's state should create a Transaction and domain evnet
    // need to valide fields (i.e. you should check if we have enough funds, etc.)
    
    public Portfolio(User user, String portfolioName, String portfolioDescription, Money initialBalance, CurrencyConversionService conversionService) {
        this(
            user, 
            new PortfolioId(UUID.randomUUID()), 
            portfolioName, 
            portfolioDescription, 
            initialBalance, 
            new HashMap<>(), 
            new HashMap<>(), 
            new ArrayList<>(), 
            new ArrayList<>(),
            conversionService
        );

        // Add PortfolioCreatedEvent
        this.domainEvents.add(new PortfolioCreatedEvent(this.portfolioId, user.getId(), initialBalance, Instant.now()));
    }

    private Portfolio(
        User user, 
        PortfolioId portfolioId, 
        String portfolioName, 
        String portfolioDescription,
        Money portfolioCashBalance, 
        Map<LiabilityId, Liability> liabilities,
        Map<AssetHoldingId, AssetHolding> holdings, 
        List<Transaction> transactions,
        List<Object> domainEvents,
        CurrencyConversionService conversionService
    ) {
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        this.portfolioName = validatePortfolioName(portfolioName);
        this.portfolioDescription = portfolioDescription; // Can be null
        this.portfolioCashBalance = Objects.requireNonNull(portfolioCashBalance, "Initial balance cannot be null");
        this.liabilities = Objects.requireNonNull(liabilities, "Liabilities map cannot be null");
        this.holdings = Objects.requireNonNull(holdings, "Holdings map cannot be null");
        this.transactions = Objects.requireNonNull(transactions, "Transactions list cannot be null");
        this.domainEvents = Objects.requireNonNull(domainEvents, "Domain events list cannot be null");
        this.conversionService = Objects.requireNonNull(conversionService, "Currency conversion service cannot be null");
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

    //for assets we use the asset currency, else we use portfolio currency
    private Money calculateTotalFees(List<Fee> fees, Currency targetCurrency, Instant transactionDate) {
        if (fees == null || fees.isEmpty()) {
            return Money.ZERO(targetCurrency);
        }

        Money totalFees = Money.ZERO(targetCurrency);

        for (Fee fee : fees) {
            Money feeAmount = fee.amount();
            
            // If the fee's currency is different from the target currency, convert it
            if (!feeAmount.currency().equals(targetCurrency)) {
                feeAmount = conversionService.convert(feeAmount, targetCurrency, transactionDate);
            }
            
            totalFees = totalFees.add(feeAmount);
        }
        return totalFees;
    }

    private Optional<AssetHolding> findAssetHolding(AssetIdentifier assetIdentifier) {
        return this.holdings.values().stream()
            .filter(ah -> ah.getAssetIdentifier().equals(assetIdentifier))
            .findFirst();
    }

    private AssetHolding createNewAssetHolding(AssetIdentifier assetIdentifier) {
        AssetHolding newHolding = new AssetHolding(
            new AssetHoldingId(UUID.randomUUID()), 
            this.portfolioId, 
            assetIdentifier, 
            BigDecimal.ZERO, 
            portfolioCashBalance, 
            Instant.now() 
        );

        this.holdings.put(newHolding.getAssetHoldingId(), newHolding);
        return newHolding;
    }
    
    private void validateSufficientFunds(Money requiredAmount, String operation) {
        if (this.portfolioCashBalance.add(requiredAmount).isNegative()) {
            throw new InsufficientFundsException("Insufficient cash for " + operation + ". Required: " + 
                requiredAmount.negate() + ", Available: " + this.portfolioCashBalance);
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
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per unit must be positive");
        }

        Money totalFees = calculateTotalFees(nativeFees, pricePerUnit.currency(), transactionDate);
        Money assetCost = pricePerUnit.multiply(quantity);

        // Now, add them together in the same currency
        Money totalCostInAssetCurrency = assetCost.add(totalFees);

        Money totalCostPortfolioCurrency = conversionService.convert(totalCostInAssetCurrency, this.portfolioCashBalance.currency(), transactionDate);
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
            nativeFees == null || nativeFees.isEmpty() ? Collections.emptyList() : nativeFees, 
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
            transactionDate // Fixed: was Instant.now()
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
    }

    public void sellAsset(
        AssetHoldingId assetHoldingId,
        BigDecimal quantityToSell,
        Money pricePerUnit,  // native currency
        List<Fee> nativeFees,
        Instant transactionDate,
        TransactionSource source,
        String description
    ) {
         // Validation
        Objects.requireNonNull(assetHoldingId, "Asset holding ID cannot be null");
        Objects.requireNonNull(quantityToSell, "Quantity to sell cannot be null");
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null");
        Objects.requireNonNull(source, "Transaction source cannot be null");
        
        if (quantityToSell.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to sell must be positive");
        }

        AssetHolding assetHolding = this.holdings.get(assetHoldingId);
        if (assetHolding == null) {
            throw new AssetNotFoundException("Cannot sell asset not held in portfolio");
        }
        if (assetHolding.getQuantity().compareTo(quantityToSell) < 0) {
            throw new IllegalArgumentException("Cannot sell more units than you have. Available: " + 
                assetHolding.getQuantity() + ", Requested: " + quantityToSell);
        }

        Money totalFees = calculateTotalFees(nativeFees, pricePerUnit.currency(), transactionDate);
        Money grossProceeds = pricePerUnit.multiply(quantityToSell);
        Money netCashImpact = grossProceeds.subtract(totalFees);

        assetHolding.removeFromPosition(quantityToSell);
        
        // Clean up empty holdings
        if (assetHolding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            this.holdings.remove(assetHoldingId);
        }

        Money netCashImpactPortfolioCurrency = conversionService.convert(netCashImpact, this.portfolioCashBalance.currency(), transactionDate);
        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpactPortfolioCurrency);

        TradeExecutionTransactionDetails details = new TradeExecutionTransactionDetails(
            assetHolding.getAssetIdentifier(),
            quantityToSell.negate(), 
            pricePerUnit,
            source,
            description,
            nativeFees,
            this.portfolioCashBalance.currency(),
            assetHoldingId,
            this.conversionService
        );

        Transaction transaction = new Transaction(
            new TransactionId(UUID.randomUUID()),
            new CorrelationId(UUID.randomUUID()),
            null,
            this.portfolioId,
            TransactionType.SELL,
            TransactionStatus.COMPLETED,
            details,
            netCashImpactPortfolioCurrency, // Fixed: was netCashImpact in native currency
            transactionDate,
            transactionDate // Fixed: was Instant.now()
        );

        this.transactions.add(transaction);

        AssetSoldEvent event = new AssetSoldEvent(
            this.portfolioId,
            assetHoldingId,
            assetHolding.getAssetIdentifier(),
            quantityToSell,
            grossProceeds,
            transactionDate // Fixed: was Instant.now()
        );

        this.domainEvents.add(event);
    }

    // note we will have to do the same thing for the Liability
    public LiabilityId  incurrNewLiability(
        LiabilityDetails liabilityDetails,
        Money liabilityAmount,
        TransactionSource source,
        List<Fee> fees,
        Instant incurrenceDate
    ) {
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

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
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
            transactionDate // Fixed: was Instant.now()
        );
        
        this.transactions.add(transaction);

        LiabilityPaymentRecordedEvent event = new LiabilityPaymentRecordedEvent(
            liabilityId, 
            paymentAmount, 
            result.principalPaid(), // Fixed: was paymentAmount twice
            transactionDate // Fixed: was Instant.now()
        );
        domainEvents.add(event);

    }

    public void recordCashflow(
        Money amount, // in portfolio currency
        CashflowType cashflowType,
        TransactionSource source,
        String description,
        List<Fee> fees,
        Instant transactionDate
    ) {
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
            transactionDate
        );
        this.domainEvents.add(event);
        

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
                TradeExecutionTransactionDetails buyDetails = (TradeExecutionTransactionDetails) originalTransaction.getTransactionDetails();
                AssetHolding buyHolding = holdings.get(buyDetails.getAssetHoldingId());
                // Remove the exact quantity that was originally bought.
                buyHolding.removeFromPosition(buyDetails.getQuantity().abs());
                break;
            case SELL:
                TradeExecutionTransactionDetails sellDetails = (TradeExecutionTransactionDetails) originalTransaction.getTransactionDetails();
                AssetHolding sellHolding = holdings.get(sellDetails.getAssetHoldingId());
                // Add the exact quantity back.
                // The cost basis is derived from the original transaction's details,
                // not the net impact, which includes proceeds. We need to re-calculate it.
                Money originalCostBasisForSale = sellHolding.getAverageACBPerUnit().multiply(sellDetails.getQuantity());
                sellHolding.addToPosition(sellDetails.getQuantity().abs(), originalCostBasisForSale);
                break;
            case LIABILITY_INCURRENCE:
                LiabilityId liabilityId = ((LiabilityIncurrenceTransactionDetails) originalTransaction.getTransactionDetails()).getLiabilityId();
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
            new CorrelationId(UUID.randomUUID()),
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

    public void updateLiability(LiabilityId liabilityId, LiabilityDetails newDetails) {
        Liability existingLiability = liabilities.get(liabilityId);

        existingLiability.updateDetails(newDetails);
    }

    public void updatePortfolioDetails(String newName, String newDescription) {
        if (newName != null) {
            this.portfolioName = validatePortfolioName(newName);
        }
        if (newDescription != null) {
            this.portfolioDescription = newDescription;
        }
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // --- GETTERS ---
    public User getUser() {
        return user;
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

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }

    public CurrencyConversionService getConversionService() {
        return conversionService;
    }
    
}