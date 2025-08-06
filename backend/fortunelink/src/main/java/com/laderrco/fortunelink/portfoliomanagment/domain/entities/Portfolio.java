package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TradeExecutionTransactionDetails;

/*
 * <<Entities>>
 * Portfolio
 * AssetHolding
 * Liability
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
 * PaymentAllocationResult
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
}