package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;

public class IncomeTransactionDetails extends TransactionDetails {
    /*
     * for handling passive icnome events like dividends or interest payments
     */

    private final AssetIdentifier assetIdentifier; // the asset that generated the income
    private final Money amount; // amount of money received in native currency
    private final AssetHoldingId assetHoldingId;
    // should have an incomeType Enum, but most of the values are in the TransactionType Enum

    protected IncomeTransactionDetails(TransactionSource source, String description, List<Fee> fees) {
        super(source, description, fees);
        //TODO Auto-generated constructor stub
    }
    
}
