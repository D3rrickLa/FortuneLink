package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Portfolio {
    private final UUID portfolioId;
    private final UUID userId;
    private String portfolioName;
    private String portfolioDescription;
    private Money portoflioCashBalance;
    private Currency currencyPreference; // view everything in one currency, preference
    private List<Fee> fees;
    private List<Transaction> transactions;
    private List<AssetHolding> assetHoldings;
    private List<Liability> liabilities;

    public void recordCashflow() {

    }

    public void recordAssetPurchase() {

    }

    public void recordAssetSale() {

    }

    public void recordNewLiability() {

    }

    public void recordLiabilityPayment() {

    }

    public void reverseTransaction() {

    }

    

}
