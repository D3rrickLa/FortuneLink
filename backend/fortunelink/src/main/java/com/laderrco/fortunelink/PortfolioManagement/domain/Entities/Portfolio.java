package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

// Aggregate Root
public class Portfolio {
    private UUID portfolioId;
    private UUID userId;
    private String name;
    private Currency currencyPref;

    private List<AssetHolding> assets;
    private List<Liability> liabilities;
    private List<Transaction> transactions;
    private CurrencyPreference CurrencyPreference;
    
    // what can a Portfolio do?
    /*
     * Update itself with the holdings
     * Remove holdings
     * be renamed to something
     * it has boundaries, so anything connected to it should be but here
     */

    public Portfolio(UUID portfolioUuid, UUID userUuid, String name) {
        this.portfolioId = portfolioUuid;
        this.userId = userUuid;
        this.name = name;
    }


    public void deletePortfolio() {
        // do something
    }

    public void renamePortfolio(String newName) {
        this.name = newName;
    }

    public List<AssetHolding> viewAssetHoldings() {
        // shows a basic view of all current holdings
        // their current price, their bought price, and shares owned
        return null;
    }

    public AssetHolding recordNewAssetPurchase() {
        return null;
    }

    public void recordAssetSale() {
        
    }

    public Liability addLiability() {
        return null;
    }

    public void recordNewLiabilityPayment() {

    }

    // for non asset/liability transaction
    public void recordCashTransaction() {

    }

    
}
