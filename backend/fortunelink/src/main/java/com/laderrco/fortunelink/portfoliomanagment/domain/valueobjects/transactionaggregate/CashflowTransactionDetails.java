package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public final class CashflowTransactionDetails extends TransactionDetails {
    private final Money originalCashflowAmount;
    private final Money covertedCashflowAmount;
    private final Money totalConversionFees;
    private final ExchangeRate exchangeRate;
    
    public CashflowTransactionDetails(
        Money originalCashflowAmount, 
        Money covertedCashflowAmount,
        Money totalConversionFees,
        ExchangeRate exchangeRate
    ) {
        this.originalCashflowAmount = originalCashflowAmount;
        this.covertedCashflowAmount = covertedCashflowAmount;
        this.totalConversionFees = totalConversionFees; // in portfolio's currency
        this.exchangeRate = exchangeRate;
    }

    public Money getOriginalCashflowAmount() {return originalCashflowAmount;}
    public Money getCovertedCashflowAmount() {return covertedCashflowAmount;}
    public Money getTotalConversionFees() {return totalConversionFees;}
    public ExchangeRate getExchangeRate() {return exchangeRate;}
       
}