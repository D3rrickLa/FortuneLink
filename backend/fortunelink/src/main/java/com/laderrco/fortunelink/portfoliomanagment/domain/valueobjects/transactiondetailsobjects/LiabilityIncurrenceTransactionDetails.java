package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;

public class LiabilityIncurrenceTransactionDetails extends TransactionDetails {
    // might need a liability identifier class
    private final Money principalAmount;
    private final Percentage interestRate; // year

    protected LiabilityIncurrenceTransactionDetails(TransactionSource source, String description, List<Fee> fees) {
        super(source, description, fees);
        //TODO Auto-generated constructor stub
    }
    
}
