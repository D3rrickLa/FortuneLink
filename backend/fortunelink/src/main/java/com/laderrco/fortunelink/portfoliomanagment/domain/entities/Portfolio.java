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
 * TransactionType ✅
 * AssetType ✅
 * LiabilityType ✅
 * FeeType ✅
 * 
 * TransactionStatus ✅
 * TransactionSource ✅
 * 
 * DecimalPrecision ✅
 * CryptoSymbols <- should be replaced by either a DB to reference or a service
 * 
 * AssetIdentifier
 * AllocationItem
 * AssetAllocation
 * 
 * TransactionMetadata
 * ExchangeRate
 * 
 * Money
 * Fee
 * Percentage
 * 
 * MarketPrice
 * 
 * <<Services>>
 * CurrencyConversionService
 * MarketDataService
 * PortfolioDomainService
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