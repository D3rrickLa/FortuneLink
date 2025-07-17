package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetAllocation;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.TransactionDetails;
import com.laderrco.fortunelink.shared.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Portfolio {
    private final UUID portfolioId;
    private final UUID userId;
    private String portfolioName;
    private String portfolioDescription;
    private Money portfolioCashBalance;
    private Currency currencyPreference; // view everything in one currency, preference
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
        this.transactions = new ArrayList<>();
        this.assetHoldings = new ArrayList<>();
        this.liabilities = new ArrayList<>();

        this.exchangeRateService = exchangeRateService;
    }

    private Money getTotalFeesAmount(CommonTransactionInput commonTransactionInput) {
        Money totalFeesAmount = Money.ZERO(this.currencyPreference);
        for (Fee fee : commonTransactionInput.fees()) {
            Money feeAmount = fee.amount();
            if (!feeAmount.currency().equals(this.currencyPreference)) {
                
                feeAmount = exchangeRateService.convert(feeAmount, this.currencyPreference);
                // NOTE: fees are not expected to be in the portfolio's currency, we will be converting
                // if we did expect if to be in the currency fee amount, throw an error
            }

            totalFeesAmount = totalFeesAmount.add(feeAmount);
        }
        return totalFeesAmount;
    }

    // the parameter head might be wrong
    // do we abstract some of the Transaction methods to the TransactionDetails class?
    // we are trusting 'details' for all the pre calcualted financial gigures for a transaction
    public void recordCashflow(CashflowTransactionDetails details, CommonTransactionInput commonTransactionInput, Instant transactionDate) {

        Money totalAmount = details.getConvertedCashflowAmount();

        Money totalFeesAmount = getTotalFeesAmount(commonTransactionInput);

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
        if (Set.of(TransactionType.DEPOSIT, TransactionType.INTEREST, TransactionType.DIVIDEND).contains(commonTransactionInput.transactionType())) {
            netCashImpact = totalAmount.subtract(totalFeesAmount);
        } 
        else if (Set.of(TransactionType.WITHDRAWAL).contains(commonTransactionInput.transactionType())) {
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
    }

    public void recordAssetPurchase(AssetTransactionDetails details, CommonTransactionInput commonTransactionInput, Instant transactionDate) {
        Objects.requireNonNull(details, "details cannot be null.");
        Objects.requireNonNull(commonTransactionInput, "commonTransactionInput cannot be null.");
        Objects.requireNonNull(transactionDate, "transactionDate cannot be null.");

        if (commonTransactionInput.transactionType() != TransactionType.BUY) {
            throw new IllegalArgumentException("Expected BUY transaction type, got: " + commonTransactionInput.transactionType());
        }

        Money totalTransactionAmount = details.getAssetValueInPortfolioCurrency();
        // Money totalFeesInPortfolioCurrency = getTotalFeesAmount(commonTransactionInput); // this is the problem child, we already calculated this
        // it might be giving us wrong values
        
        Optional<AssetHolding> existingHolding = assetHoldings.stream()
            .filter(ah -> ah.getAssetIdentifier().equals(details.getAssetIdentifier()))
            .findFirst();
        AssetHolding holding;
        if (existingHolding.isPresent()) {
            holding = existingHolding.get();
            holding.addToPosition(details.getQuantity(), details.getCostBasisInAssetCurrency());
        }
        else {
            holding = new AssetHolding(
                UUID.randomUUID(),
                this.portfolioId,
                details.getAssetIdentifier(),
                details.getQuantity(),
                details.getCostBasisInAssetCurrency(),
                transactionDate
            );
            this.assetHoldings.add(holding);
        }

        // using the precalculated value instead of asset value in portfolio currency - fees in portfolio currency
        Money netCashImpact = details.getCostBasisInPortfolioCurrency().negate();
        Money newBalance = this.portfolioCashBalance.add(netCashImpact);
        if (newBalance.isNegative()) {
            throw new InsufficientFundsException("Insufficient cash for asset purchase.");
        }

        this.portfolioCashBalance = newBalance;

        Transaction newAssetTransaction = new Transaction(
            UUID.randomUUID(),
            this.portfolioId,
            commonTransactionInput.correlationId(),
            commonTransactionInput.parentTransactionId(),
            commonTransactionInput.transactionType(),
            totalTransactionAmount,
            transactionDate,
            details,
            commonTransactionInput.transactionMetadata(),
            commonTransactionInput.fees(),
            false,
            1
        );
        this.transactions.add(newAssetTransaction);
    }

    public void recordAssetSale(AssetTransactionDetails details, CommonTransactionInput commonTransactionInput, Instant transactionDate) {
        Objects.requireNonNull(details, "details cannot be null.");
        Objects.requireNonNull(commonTransactionInput, "commonTransactionInput cannot be null.");
        Objects.requireNonNull(transactionDate, "transactionDate cannot be null.");

        if (commonTransactionInput.transactionType() != TransactionType.SELL) {
            throw new IllegalArgumentException("Expected SELL transaction type, got: " + commonTransactionInput.transactionType());
        }

        Optional<AssetHolding> existingHolding = assetHoldings.stream()
            .filter(ah -> ah.getAssetIdentifier().equals(details.getAssetIdentifier()))
            .findFirst();

        if (existingHolding.isEmpty()) {
            throw new IllegalArgumentException("Cannot sell asset not held in portfolio: " + details.getAssetIdentifier().symbol());
        }

        AssetHolding holding = existingHolding.get();
        if (holding.getTotalQuantity().compareTo(details.getQuantity()) < 0) {
            throw new IllegalArgumentException("Cannot sell more units than you have.");
        }

        
        Money netCashImpact = details.getAssetValueInPortfolioCurrency().subtract(details.getTotalFeesInPortfolioCurrency());
        if (!this.portfolioCashBalance.currency().equals(netCashImpact.currency())) {
            throw new IllegalArgumentException("Portfolio cash balance currency does not match transaction's net cash impact currency.");
        }
        this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact); // Cash increases
        
        holding.removeFromPosition(details.getQuantity());
        Transaction newAssetTransaction = new Transaction(
                UUID.randomUUID(),
                this.portfolioId,
                commonTransactionInput.correlationId(),
                commonTransactionInput.parentTransactionId(),
                commonTransactionInput.transactionType(), // Should be SELL
                details.getAssetValueInPortfolioCurrency(), // Store the gross proceeds as the transaction amount
                transactionDate,
                details,
                commonTransactionInput.transactionMetadata(),
                commonTransactionInput.fees(), // Store the individual fees
                false, 
                1
        );
        this.transactions.add(newAssetTransaction);
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
        // Money total = Money.ZERO(this.currencyPreference);
        // for (AssetHolding assetHolding : assetHoldings) {
        //     // check if we need to even do it
        //     MarketPrice price = currentPrices.get(assetHolding.getAssetIdentifier());

        //     if (price != null) {
        //         Money holdingValue = assetHolding.getCurrentValue(price);
        //         Money convertedValue = exchangeRateService.convert(holdingValue, this.currencyPreference);
        //         total = total.add(convertedValue);
        //     }
        // }
        // return total;
        return null;
    }   
    
    public Money calculateUnrealizedGains(AssetAllocation currentPrices) {
        return null;
    }

    public AssetAllocation getAssetAllocation (Map<AssetIdentifier, MarketPrice> currentPrices) {
        return null;
        // Money totalValue = calculateTotalValue(currentPrices);
        // AssetAllocation allocation = new AssetAllocation(totalValue, this.currencyPreference);

        // for (AssetHolding assetHolding : assetHoldings) {
        //     MarketPrice marketPrice = currentPrices.get(assetHolding.getAssetIdentifier()); 
        //     if (marketPrice != null) {
        //         Money holdingValue = assetHolding.getCurrentValue(marketPrice); // this needs to return value in portfolio pref
                
        //         Money covertedValue = exchangeRateService.convert(holdingValue, this.currencyPreference);
                
        //         Percentage percentage = new Percentage(
        //             covertedValue.amount()
        //             .divide(totalValue.amount(), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP)
        //             .multiply(BigDecimal.valueOf(100))
        //         );

        //         allocation.addAllocation(assetHolding.getAssetIdentifier(), covertedValue, percentage);
        //     }
        // }

        // return allocation;
    } 

    public void accruelInterestLiabilities() {

    }
    
    public UUID getPortfolioId() {return portfolioId;}
    public UUID getUserId() {return userId;}
    public String getPortfolioName() {return portfolioName;}
    public String getPortfolioDescription() {return portfolioDescription;}
    public Money getPortfolioCashBalance() {return portfolioCashBalance;}
    public Currency getCurrencyPreference() {return currencyPreference;}
    public List<Transaction> getTransactions() {return transactions;}
    public List<AssetHolding> getAssetHoldings() {return assetHoldings;}
    public List<Liability> getLiabilities() {return liabilities;}
    public ExchangeRateService getExchangeRateService() {return exchangeRateService;}
    
}
