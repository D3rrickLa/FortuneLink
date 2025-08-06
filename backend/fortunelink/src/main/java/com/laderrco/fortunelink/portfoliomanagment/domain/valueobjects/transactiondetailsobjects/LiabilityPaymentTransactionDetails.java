package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

// liabilities should be in Portoflio Currency pref
public class LiabilityPaymentTransactionDetails {
    private final Money principalPaymentAmount;
    private final Money interestPaymentAmount;
}
