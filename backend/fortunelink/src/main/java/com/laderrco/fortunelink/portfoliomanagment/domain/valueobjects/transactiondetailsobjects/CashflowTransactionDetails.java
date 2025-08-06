package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;

public class CashflowTransactionDetails extends TransactionDetails {
    /*
     * handles transaction the only impact the cash balance of a portfolio such as deposits and withdrawals
     * we can only deposit and withdrawal money in our own currency, will need to convert
     */
    private final Money amount;

    

    protected CashflowTransactionDetails(
        Money amount,

        TransactionSource source, 
        String description, 
        List<Fee> fees
        ) {
        super(source, description, fees);
        
        this.amount = amount;
    }
    
}
