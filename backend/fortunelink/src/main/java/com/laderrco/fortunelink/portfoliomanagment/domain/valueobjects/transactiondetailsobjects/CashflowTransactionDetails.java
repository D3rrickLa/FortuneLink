package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

// this is for cash related events ONLY so other fees don't go here, just cash conversion fees
public final class CashflowTransactionDetails extends TransactionDetails {
    private final Money originalCashflowAmount;
    private final Money convertedCashflowAmount;
    private final Money totalConversionFees; 
    private final ExchangeRate exchangeRate;
    
    public CashflowTransactionDetails(
        Money originalCashflowAmount, 
        Money convertedCashflowAmount,
        Money totalConversionFees,
        ExchangeRate exchangeRate
    ) {
        Objects.requireNonNull(originalCashflowAmount, "Original cashflow amount cannot be null.");
        Objects.requireNonNull(convertedCashflowAmount, "Converted cashflow amount cannot be null.");
        Objects.requireNonNull(totalConversionFees, "Total conversion fees cannot be null.");
        // Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null.");   

        this.originalCashflowAmount = originalCashflowAmount;
        this.convertedCashflowAmount = convertedCashflowAmount;
        this.totalConversionFees = totalConversionFees; // in portfolio's currency
        this.exchangeRate = exchangeRate;
    }

    public Money getOriginalCashflowAmount() {return originalCashflowAmount;}
    public Money getConvertedCashflowAmount() {return convertedCashflowAmount;}
    public Money getTotalConversionFees() {return totalConversionFees;}
    public ExchangeRate getExchangeRate() {return exchangeRate;}
       
}