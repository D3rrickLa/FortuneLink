package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

/*
 * <<Entities>>
 * Portfolio
 * AssetHolding
 * Liability
 * Transaction
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
 * AssetIdentifier
 * CommonTransactionInput (removed, in TransactionDetails)
 * ExchangeRate ✅
 * Fee ✅
 * MarketPrice
 * Money ✅
 * PaymentAllocationResult
 * Percentage ✅
 * TransactionMetadata (removed, in TransactionDetails)
 * 
 * <<TransactionDetails>>
 * TransactionDetails
 * 
 * <<Services>>
 * CurrencyConversionService 🟨
 * MarketDataService 🟨
 * PortfolioDomainService 🟨
 */

public class Portfolio {
    /*
     * Track assets 
     * manage cash balance
     * event - dividends, interest, etc.
     * updating general info
     * handle liabilities
     */
    
}