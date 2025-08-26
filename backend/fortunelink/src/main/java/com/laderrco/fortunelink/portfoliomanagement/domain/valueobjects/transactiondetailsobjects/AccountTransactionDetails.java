package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.util.List;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;

public class AccountTransactionDetails extends TransactionDetails {

    protected AccountTransactionDetails(TransactionSource source, String description, List<Fee> fees) {
        super(source, description, fees);
        //TODO Auto-generated constructor stub
    }
    
}
