package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;

public final class TradeExecutionTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money pricePerUnit; // in native's currency
    private final Money assetValueInNativeCurrency; // quantity * ppu
    private final Money assetValueInPortfolioCurrency; // at time of purchase
    private final Money totalFeesInPortfolioCurrency;
    private final AssetHoldingId assetHoldingId;
    private final Money realizedGainLossAssetCurrency;
    private final Money realizedGainLossPortfolioCurrency;
    private final Money acbPerUnitAtSale; // Store ACB per unit at time of sale for reversals

    // cost basis is calculated with the follow: assetValue + totalFees 
    // update ^^^ this is a property of the AssetHolding
    // for the fees the TRansaction Entity itself would have a Service to handle

    public TradeExecutionTransactionDetails(
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit,
        TransactionSource source,
        String description,
        List<Fee> nativeFees,
        Currency portfolioCurrency,
        AssetHoldingId assetHoldingId,
        CurrencyConversionService currencyConversionService
    ) {
        super(source, description, nativeFees);

        Money convertedPrice = currencyConversionService.convert(pricePerUnit, portfolioCurrency);
        Money totalConvertedFees = nativeFees.stream()
            .map(fee -> currencyConversionService.convert(fee.amount(), portfolioCurrency))
            .reduce(Money.ZERO(portfolioCurrency), Money::add);

        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit; // Store the original for audit
        this.assetValueInNativeCurrency = pricePerUnit.multiply(quantity);
        this.assetValueInPortfolioCurrency = convertedPrice.multiply(quantity);
        this.totalFeesInPortfolioCurrency = totalConvertedFees;
        this.assetHoldingId = assetHoldingId;
        this.realizedGainLossAssetCurrency = null;
        this.realizedGainLossPortfolioCurrency = null;
        this.acbPerUnitAtSale = null; // Not applicable for buy transactions
    }

    public TradeExecutionTransactionDetails(
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit,
        TransactionSource source,
        String description,
        List<Fee> nativeFees,
        Currency portfolioCurrency,
        AssetHoldingId assetHoldingId,
        CurrencyConversionService currencyConversionService,
        Money realizedGainLossAssetCurrency,
        Money realizedGainLossPortfolioCurrency
    ) {
        super(source, description, nativeFees);

        Money convertedPrice = currencyConversionService.convert(pricePerUnit, portfolioCurrency);
        Money totalConvertedFees = nativeFees.stream()
            .map(fee -> currencyConversionService.convert(fee.amount(), portfolioCurrency))
            .reduce(Money.ZERO(portfolioCurrency), Money::add);

        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit; // Store the original for audit
        this.assetValueInNativeCurrency = pricePerUnit.multiply(quantity);
        this.assetValueInPortfolioCurrency = convertedPrice.multiply(quantity);
        this.totalFeesInPortfolioCurrency = totalConvertedFees;
        this.assetHoldingId = assetHoldingId;
        this.realizedGainLossAssetCurrency = realizedGainLossAssetCurrency;
        this.realizedGainLossPortfolioCurrency = realizedGainLossPortfolioCurrency;
        this.acbPerUnitAtSale = null; // Will be set by the Portfolio when creating sell transactions
    }

    public TradeExecutionTransactionDetails(
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit,
        TransactionSource source,
        String description,
        List<Fee> nativeFees,
        Currency portfolioCurrency,
        AssetHoldingId assetHoldingId,
        CurrencyConversionService currencyConversionService,
        Money realizedGainLossAssetCurrency,
        Money realizedGainLossPortfolioCurrency,
        Money acbPerUnitAtSale
    ) {
        super(source, description, nativeFees);

        Money convertedPrice = currencyConversionService.convert(pricePerUnit, portfolioCurrency);
        Money totalConvertedFees = nativeFees.stream()
            .map(fee -> currencyConversionService.convert(fee.amount(), portfolioCurrency))
            .reduce(Money.ZERO(portfolioCurrency), Money::add);

        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit; // Store the original for audit
        this.assetValueInNativeCurrency = pricePerUnit.multiply(quantity);
        this.assetValueInPortfolioCurrency = convertedPrice.multiply(quantity);
        this.totalFeesInPortfolioCurrency = totalConvertedFees;
        this.assetHoldingId = assetHoldingId;
        this.realizedGainLossAssetCurrency = realizedGainLossAssetCurrency;
        this.realizedGainLossPortfolioCurrency = realizedGainLossPortfolioCurrency;
        this.acbPerUnitAtSale = acbPerUnitAtSale;
    }


    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getPricePerUnit() {
        return pricePerUnit;
    }

    public Money getAssetValueInNativeCurrency() {
        return assetValueInNativeCurrency;
    }

    public Money getAssetValueInPortfolioCurrency() {
        return assetValueInPortfolioCurrency;
    }

    public Money getTotalFeesInPortfolioCurrency() {
        return totalFeesInPortfolioCurrency;
    }

    public AssetHoldingId getAssetHoldingId() {
        return assetHoldingId;
    }

    public Money getRealizedGainLossAssetCurrency() {
        return realizedGainLossAssetCurrency;
    }

    public Money getRealizedGainLossPortfolioCurrency() {
        return realizedGainLossPortfolioCurrency;
    }

    public Money getAcbPerUnitAtSale() {
        return acbPerUnitAtSale;
    }
    
}
