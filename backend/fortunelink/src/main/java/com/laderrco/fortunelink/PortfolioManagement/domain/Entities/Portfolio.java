package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Percentage;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;


public class Portfolio {
    private final UUID portfolioId;
    private final UUID userId; // FK from supabase
    private String portfolioName;
    private String portfolioDescription;
    private PortfolioCurrency portfolioCurrencyPreference; // your currency preference can change
    private Money portfolioCashBalance; // cash in your portfolio, we need to make sure you have cash to spend on assets, else you can't make new transaction
    
    private final Instant createdAt;
    private Instant updatedAt;
    
    private List<Transaction> transactions;
    private List<AssetHolding> assetHoldings;
    private List<Liability> liabilities;
    
    public Portfolio(final UUID portfolioId, final UUID userId, String portfolioName, String portfolioDescription, PortfolioCurrency portfolioCurrencyPreference, Money portfolioCashBalance, Instant createdAt, Instant updatedAt) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.portfolioName = portfolioName;
        this.portfolioDescription = portfolioDescription;
        this.portfolioCurrencyPreference = portfolioCurrencyPreference;
        this.portfolioCashBalance = portfolioCashBalance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.transactions = new ArrayList<>();
        this.assetHoldings = new ArrayList<>();
        this.liabilities = new ArrayList<>();
    }

    /*
     * Behaviour - what can a portfolio do?
     * in our case we have 3 items that interact with porfolio (Transaction, AssetHolding, Liability)
     * so we need to have methods for adding and 'removing' said things from each entity
     * we also need to deal with cashflow, how can we get cash in to/out of our portfolio?
     * - deposit
     * - withdrawl
     * - dividend
     * - interest
     * 
     * for such behaviours, whenever you are stuck, think of 'verbs' related to the Portoflio and things you want to do
     * i.e. buying/sell stocks
     * i.e. depositing/withdrawing money
     * etc.
     * 
     * for what is a portfolio? think of 'nouns'
     * i.e. it's a place to containizer all your finances so Cash in portfolio, investments, liabilities, transactions, etc.
     * 
     * NOTE: 
     * since we are pulling our 'funds' from 1 cash balance, we need to deal with currency conversion, so whenever someone enters a trade that isn't today,
     * we need to pull historical info for that exchange rate. Think of this like WealthSimple without a USD account, we can buy only with CAD, but we can convert
     * our CAD to USD to buy something else. This is in contrast where we have dedicated accounts for USD and CAD
    */


    // what do we need to to add/remove cash to our account? actual cash
    public void recordCashflow(TransactionType type, Money cashflowAmount, Instant cashflowEventDate) {
        
    }
    
    // NOTE: will need to handle the currency conversion of stuff so if bought in USD, we need to update our currencyPerference
    // also need ot deduct from teh cashBalance, hence the reason for the conversion
    // we will need some sort of service to do the conversion
    public AssetHolding recordAssetHoldingPurchase(final UUID portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantityOfAssetBought, Instant acquisitionDate, Money pricePerUnit) {
        return null;
        
    }
    
    public void recordAssetHoldingSale(AssetIdentifier assetIdentifier, BigDecimal quantityToSell, Money salePricePerUnit) {

    }

    public Liability addNewLiability(final UUID portfolioId, String liabilityName, String liabilityDescription, Money initialOutstandingBalance, Percentage interestRate, Instant maturityDate) {
        return null;
    }

    public void recordLiabilityPayment(final UUID portfolioId, final UUID liabilityId, Money paymentAmount, Instant transactionDate) {

    }

    public void voidTransaction(final UUID transactionId, String reason) {

    }





    // --- Getter Methods --- //
    public UUID getPortfolioId() {
        return portfolioId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public String getPortfolioDescription() {
        return portfolioDescription;
    }

    public PortfolioCurrency getPortfolioCurrencyPreference() {
        return portfolioCurrencyPreference;
    }

    public Money getPortfolioCashBalance() {
        return portfolioCashBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<AssetHolding> getAssetHoldings() {
        return assetHoldings;
    }

    public List<Liability> getLiabilities() {
        return liabilities;
    }


}
