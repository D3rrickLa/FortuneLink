package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetAllocation;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.TransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class Portfolio {
    private final UUID portfolioId;
    private final UUID userId;
    private String portfolioName;
    private String portfolioDescription;
    private Money portfolioCashBalance;
    private Currency currencyPreference; // view everything in one currency, preference
    private List<Fee> fees;
    private List<Transaction> transactions;
    private List<AssetHolding> assetHoldings;
    private List<Liability> liabilities;

    private final ExchangeRateService exchangeRateService;

    public Portfolio(
        UUID portfolioId, 
        UUID userId, 
        String portfolioName, 
        String portfolioDescription,
        Money portfolioCashBalance,
        Currency currencyPreference,
        ExchangeRateService exchangeRateService
        ) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.portfolioName = portfolioName;
        this.portfolioDescription = portfolioDescription;
        this.portfolioCashBalance = portfolioCashBalance;
        this.currencyPreference = currencyPreference;
        this.fees = new ArrayList<>();
        this.transactions = new ArrayList<>();
        this.assetHoldings = new ArrayList<>();
        this.liabilities = new ArrayList<>();

        this.exchangeRateService = exchangeRateService;
    }

    // the parameter head might be wrong
    // do we abstract some of the Transaction methods to the TransactionDetails class?
    public void recordCashflow(CashflowTransactionDetails details, CommonTransactionInput commonTransactionInput, Instant transactionDate) {

        Money totalAmount = details.getCovertedCashflowAmount();

        // this is technically wrong, we need to make sure all fee are/is in the portfolio's preference
        Money totalFeesAmount = commonTransactionInput.fees().stream()
            .map((Fee::amount))
            .reduce(Money.ZERO(this.currencyPreference), Money::add);

        Transaction newCashTransaction = new Transaction(
            UUID.randomUUID(),
            this.portfolioId,
            commonTransactionInput.correlationId(),
            commonTransactionInput.parentTransactionId(),
            commonTransactionInput.transactionType(),
            totalAmount,
            transactionDate,
            details, 
            commonTransactionInput.transactionMetadata(),
            commonTransactionInput.fees(),
            false,
            1
        );

        this.transactions.add(newCashTransaction);

        Money netCashImpact = Money.ZERO(this.currencyPreference);
        if (commonTransactionInput.transactionType() == TransactionType.DEPOSIT) {
            netCashImpact = totalAmount.subtract(totalFeesAmount);
        } 
        else if (commonTransactionInput.transactionType() == TransactionType.WITHDRAWAL) {
            // For withdrawal, both the withdrawn amount and fees are subtracted.
            // Assuming principalTransactionAmount is positive (amount withdrawn).
            netCashImpact = totalAmount.negate().subtract(totalFeesAmount);
        } 
        else {
            // Handle other cashflow types or throw an exception if type is not recognized for cash balance update.
            // For example, an `ACCOUNT_TRANSFER` might have a different net impact.
            throw new IllegalArgumentException("Unsupported cashflow transaction type for balance update: " + commonTransactionInput.transactionType());
        }

        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);

        this.fees.addAll(commonTransactionInput.fees());

    }

    public void recordAssetPurchase(TransactionDetails details) {

    }

    public void recordAssetSale(TransactionDetails details) {

    }

    public void recordNewLiability(TransactionDetails details) {

    }

    public void recordLiabilityPayment(TransactionDetails details) {

    }

    public void reverseTransaction(UUID transactionId, String reason) {

    }

    public Money calculateTotalValue(Map<AssetIdentifier, MarketPrice> currentPrices) {
        // total value in portfolio's preference
        // this is an example, we would need an acutal service class to handle this
        // TODO switch to acutal service, currenyl using CAD -> USD
        Money total = Money.ZERO(this.currencyPreference);
        for (AssetHolding assetHolding : assetHoldings) {
            // check if we need to even do it
            MarketPrice price = currentPrices.get(assetHolding.getAssetIdentifier());

            if (price != null) {
                Money holdingValue = assetHolding.getCurrentValue(price);
                Money convertedValue = exchangeRateService.convert(holdingValue, this.currencyPreference);
                total = total.add(convertedValue);
            }
        }
        return total;
    }   
    
    public Money calculateUnrealizedGains(AssetAllocation currentPrices) {
        return null;
    }

    public AssetAllocation getAssetAllocation (Map<AssetIdentifier, MarketPrice> currentPrices) {
        Money totalValue = calculateTotalValue(currentPrices);
        AssetAllocation allocation = new AssetAllocation(totalValue, this.currencyPreference);

        for (AssetHolding assetHolding : assetHoldings) {
            MarketPrice marketPrice = currentPrices.get(assetHolding.getAssetIdentifier()); 
            if (marketPrice != null) {
                Money holdingValue = assetHolding.getCurrentValue(marketPrice); // this needs to return value in portfolio pref
                
                Money covertedValue = exchangeRateService.convert(holdingValue, this.currencyPreference);
                
                Percentage percentage = new Percentage(
                    covertedValue.amount()
                    .divide(totalValue.amount(), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                );

                allocation.addAllocation(assetHolding.getAssetIdentifier(), covertedValue, percentage);
            }
        }

        return allocation;
    } 

    public void accruelInterestLiabilities() {

    }

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

    public Money getPortfolioCashBalance() {
        return portfolioCashBalance;
    }

    public Currency getCurrencyPreference() {
        return currencyPreference;
    }

    public List<Fee> getFees() {
        return fees;
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

    public ExchangeRateService getExchangeRateService() {
        return exchangeRateService;
    }
    
}
