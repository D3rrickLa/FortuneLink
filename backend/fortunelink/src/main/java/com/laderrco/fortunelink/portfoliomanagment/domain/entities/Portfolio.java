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
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityIncurredEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityPaymentRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TradeExecutionTransactionDetails;

/*
 * <<Entities>>
 * Portfolio
 * AssetHolding
 * Liability ✅
 * Transaction 🟨
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
 * DividendReceivedEvent
 * PortfolioCreatedEvent
 * LiabilityPaymentRecordedEvent
 * LiabilityIncurredEvent
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
    private Money portfolioCashBalance; // can get currency pref from this, don't know if we need a sep field

    private Map<LiabilityId, Liability> liabilities;
    private Map<AssetHoldingId, AssetHolding> holdings; // for performance and convenience we use a map
    private List<Transaction> transactions; // record of events that happened, generally immutable and for historical data. that's why list
    
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
            new HashMap<LiabilityId, Liability>(), 
            new HashMap<AssetHoldingId, AssetHolding>(), 
            new ArrayList<Transaction>(), 
            new ArrayList<Object>(),
            conversionService
        );

        // there should be a domain event here
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
        this.user = user;
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.portfolioDescription = portfolioDescription;
        this.portfolioCashBalance = portfolioCashBalance;
        this.liabilities = liabilities;
        this.holdings = holdings;
        this.transactions = transactions;
        this.domainEvents = domainEvents;
        this.conversionService = conversionService;
    }

    private Money calculateTotalFees(List<Fee> fees, Instant transactionDate) {
        if (fees == null || fees.isEmpty()) {
            return Money.ZERO(this.portfolioCashBalance.currency());
        }

        Money totalFees = Money.ZERO(this.portfolioCashBalance.currency());
        Currency portfolioCurrency = this.portfolioCashBalance.currency();

        for (Fee fee : fees) {
            Money feeAmount = fee.amount();
            
            // If the fee's currency is different from the portfolio's, convert it
            if (!feeAmount.currency().equals(portfolioCurrency)) {
                feeAmount = conversionService.convert(feeAmount, portfolioCurrency, transactionDate);
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

    public void buyAsset(
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit, // in native currency
        List<Fee> nativeFees,
        Instant transactionDate,
        TransactionSource source,
        String description
    ) {
        // this needs to do the following
        /*
         * calculate the total cost (price * quant + fees)
         * update the assetholding quant/costBasis
         * create and add new Transaction to our list
         * register a domain event
         */

        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");

        // all of these variables are in the native currency
        Money totalFees = calculateTotalFees(nativeFees, transactionDate); // returns total fees in native currency
        Money assetCost = pricePerUnit.multiply(quantity);
        Money totalCost = assetCost.add(totalFees); // total cash outflow, and trust cost basis

        Money totalCostPortfolioCurrency = conversionService.convert(totalCost, this.portfolioCashBalance.currency(), transactionDate);
        Money netCashImpact = totalCostPortfolioCurrency.negate();
        
        if (this.portfolioCashBalance.add(netCashImpact).isNegative()) {
            throw new InsufficientFundsException("Insufficient cash for asset purchase.");
        }
        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);

        AssetHolding assetHolding = findAssetHolding(assetIdentifier)
            .orElseGet(() -> createNewAssetHolding(assetIdentifier));

        // that AddToPosition method should recieve thet total cost of the new acqusisition
        assetHolding.addToPosition(quantity, totalCost);

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

        // create transaction and add it to the portfolio
        Transaction transaction = new Transaction(
            new TransactionId(UUID.randomUUID()), 
            new CorrelationId(UUID.randomUUID()), 
            null, 
            this.portfolioId, 
            TransactionType.BUY, 
            TransactionStatus.COMPLETED, 
            details, 
            pricePerUnit, 
            transactionDate, 
            transactionDate
        );

        this.transactions.add(transaction);

        AssetBoughtEvent event = new AssetBoughtEvent(
            this.portfolioId,
            assetHolding.getAssetHoldingId(),
            assetIdentifier,
            quantity,
            totalCost,
            Instant.now()
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
        AssetHolding assetHolding = this.holdings.get(assetHoldingId);
        if (assetHolding == null) {
            throw new AssetNotFoundException("Cannot sell asset not held in portfolio.");
        }
        if (assetHolding.getQuantity().compareTo(quantityToSell) < 0) {
            throw new IllegalArgumentException("Cannot sell more units than you have.");
        }

        Money totalFees = calculateTotalFees(nativeFees, transactionDate);
        Money grossProceeds = pricePerUnit.multiply(quantityToSell);
        Money netCashImpact = grossProceeds.subtract(totalFees);

        assetHolding.removeFromPosition(quantityToSell);
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
            netCashImpact,
            transactionDate,
            Instant.now()
        );

        this.transactions.add(transaction);

        AssetSoldEvent event = new AssetSoldEvent(
            this.portfolioId,
            assetHoldingId,
            assetHolding.getAssetIdentifier(),
            quantityToSell,
            grossProceeds,
            Instant.now()
        );

        this.domainEvents.add(event);
    }

    // note we will have to do the same thing for the Liability
    public void incurrNewLiability(
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

        Money summedFees = calculateTotalFees(fees, incurrenceDate); // the method actual should exist in TransactionDetails.java 
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

    }

    public void recordLiabilityPayment(
        LiabilityId liabilityId, 
        Money paymentAmount,
        TransactionSource source,
        List<Fee> fees,
        Instant transactionDate

    ) {
        // some event for recording a payment
        Liability liability = this.liabilities.get(liabilityId);
        if (liability == null) {
            throw new IllegalArgumentException("Liability not found.");
        }

        PaymentAllocationResult result = liability.recordPayment(paymentAmount, Instant.now()); // time should be passed

        Money totalFees = calculateTotalFees(fees, transactionDate);

        LiabilityPaymentTransactionDetails details = new LiabilityPaymentTransactionDetails( // should change this to add LiabilityType Enum
            liabilityId, 
            result.principalPaid(),
            result.interestPaid(),
            source, 
            "Liability payment", 
            fees
        );
        
        Money cashImpact = paymentAmount.negate().subtract(totalFees); // fees are part of the total cash impact
        this.portfolioCashBalance = this.portfolioCashBalance.add(cashImpact);
        
        Transaction transaction = new Transaction (
            new TransactionId(UUID.randomUUID()),
            new CorrelationId(UUID.randomUUID()),
            null, // parent Id
            this.portfolioId,
            TransactionType.PAYMENT,
            TransactionStatus.COMPLETED,
            details,
            cashImpact,
            transactionDate,
            Instant.now()
        );
        
        this.transactions.add(transaction);

        // at the end of the transaciton, an application service would dispatch these events...
        // but how?
        // ans: after the app service loads Portfolio from the repo, it can call one of the methods in Portfolio
        // after that, it than calls the getDomainEvents to get the list of new events
        // it then uses a separate domain event dispatcher to publish these events to any listening services
        LiabilityPaymentRecordedEvent event = new LiabilityPaymentRecordedEvent(liabilityId, paymentAmount, paymentAmount, Instant.now());
        domainEvents.add(event);

    }

    public void updateLiability(LiabilityId liabilityId, LiabilityDetails newDetails) {
        Liability existingLiability = liabilities.get(liabilityId);

        existingLiability.updateDetails(newDetails);
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }

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
        return liabilities;
    }


    public Map<AssetHoldingId, AssetHolding> getHoldings() {
        return holdings;
    }


    public List<Transaction> getTransactions() {
        return transactions;
    }


    public CurrencyConversionService getConversionService() {
        return conversionService;
    }
    
}