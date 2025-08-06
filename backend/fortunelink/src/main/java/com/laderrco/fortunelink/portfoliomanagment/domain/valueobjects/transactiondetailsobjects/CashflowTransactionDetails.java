package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;

public class CashflowTransactionDetails extends TransactionDetails {
    /*
     * handles transaction the only impact the cash balance of a portfolio such as deposits and withdrawals
     * we can only deposit and withdrawal money in our own currency, will need to convert
     * this will also handle things like Fees and Taxes that are outside a purchase or sale of an asset
     */
    private final Money amount;
    private final CashflowType cashflowType;
    

    protected CashflowTransactionDetails(
        Money amount,
        CashflowType cashflowType,

        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        super(source, description, fees);
        
        validateParameter(amount, "Amount");
        validateParameter(cashflowType, "Cashflow type");

        this.amount = amount;
        this.cashflowType = cashflowType;
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

    public Money getAmount() {
        return amount;
    }

    public CashflowType getCashflowType() {
        return cashflowType;
    }
}
