package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.lang.foreign.Linker.Option;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TradeExecutionTransactionDetails;

/*
 * <<Entities>>
 * Portfolio
 * AssetHolding
 * Liability 🟨
 * Transaction 🟨
 * User
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
 * AssetBoughtEvent
 * DividendReceivedEvent
 * PortfolioCreatedEvent
 */

public class Portfolio {
    /*
     * Track assets 
     * manage cash balance
     * event - dividends, interest, etc.
     * updating general info
     * handle liabilities
     */
    
    private final PortfolioId portfolioId;
    private Map<LiabilityId, Liability> liabilities = new HashMap<>();
    private final List<Object> domainEvents = new ArrayList<>();



    

    public Portfolio(PortfolioId portfolioId, Map<LiabilityId, Liability> liabilities) {
        this.portfolioId = portfolioId;
        this.liabilities = liabilities;
    }

    // this is how we would handle the Trade Execution thing
    // proof of concept so I don't lose it
    public void buyAsset(
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit,
        List<Fee> nativeFees,
        Instant transactionDate,
        TransactionSource source,
        String description
    ) {
        AssetHolding assetHolding = findAssetHolding(assetIdentifier)
            .orElseGet(() -> createNewAssetHolding(assetIdentifier));

        TradeExecutionTransactionDetails details = new TradeExecutionTransactionDetails(
            assetIdentifier, 
            quantity, 
            pricePerUnit, 
            source, 
            description, 
            nativeFees, 
            null, 
            null, 
            null
        );

        // create transaction and add it to the portfolio
    }

    private Optional<AssetHolding> findAssetHolding(AssetIdentifier assetIdentifier) {
        return null;
    }

    private AssetHolding createNewAssetHolding(AssetIdentifier assetIdentifier) {
        return null;
    }

    // note we will have to do the same thing for the Liability
    public void recordNewLiability() {
        Liability liability = createNewLiability();

        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
            null,
            null,
            null,
            null,
            null,
            null
        );

        this.liabilities.put(null, liability);
        // new LiabilityIncurredEvent


        //domainEvents.add(event)
    }

    public void updateLiability(LiabilityId liabilityId, LiabilityDetails newDetails) {
        Liability existingLiability = liabilities.get(liabilityId);

        existingLiability.updateDetails(newDetails);
    }

    private Optional<Liability> findLiability() {
        return null;
    }

    private Liability createNewLiability() {
        return null;
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }
}