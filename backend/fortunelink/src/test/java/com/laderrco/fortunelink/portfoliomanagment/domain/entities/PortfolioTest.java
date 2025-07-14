package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.SimpleExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.CashflowTransactionDetails;
import com.laderrco.fortunelink.shared.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class PortfolioTest {
    private Portfolio portfolio;
    private UUID userId;
    private UUID portfolioId;
    private String name;
    private String desc;
    private Money portfolioCashBalance; 
    private Currency cad;
    private Currency usd;
    private ExchangeRateService exchangeRateService; 

    @BeforeEach
    void init() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        name = "Portfolio name ";
        desc = "some desc";
        
        cad = Currency.getInstance("CAD");
        usd = Currency.getInstance("USD");

        portfolioCashBalance = new Money(12000, cad);
        exchangeRateService = new SimpleExchangeRateService();

        portfolio = new Portfolio(
            portfolioId, userId, name, desc, portfolioCashBalance, cad, exchangeRateService);
    }

    @Test
    void testRecordCashflow() {
        // transaction details setup
        Money orignalMoney = new Money(2000, cad);
        Money convertedMoney = orignalMoney;
        Money totalConversionFee = new Money(0, cad);
        ExchangeRate exchangeRate = null;

        CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(orignalMoney, convertedMoney, totalConversionFee, exchangeRate);

        // common transaction setup
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.DEPOSIT_FEE, new Money(0.05, cad)));
        TransactionMetadata transactionMetadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "DEPOSITED MONEY", Instant.now(), 
            Instant.now()
        );

        UUID correlationId = UUID.randomUUID();
        UUID parentId = null;
        TransactionType transactionType = TransactionType.DEPOSIT;

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId, transactionType, transactionMetadata, fees);

        portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, Instant.now());
    
        assertEquals(new Money(13999.95, cad), portfolio.getPortfolioCashBalance());
        assertEquals(1, portfolio.getTransactions().size());
    }

    @Test 
    void testRecordCashflowDifferentCurrencyFees() {
        // transaction details setup
        Money orignalMoney = new Money(2000, cad);
        Money convertedMoney = orignalMoney;
        Money totalConversionFee = new Money(0, cad);
        ExchangeRate exchangeRate = null;

        CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(orignalMoney, convertedMoney, totalConversionFee, exchangeRate);

        // common transaction setup
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.DEPOSIT_FEE, new Money(0.05, usd)));
        TransactionMetadata transactionMetadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "DEPOSITED MONEY", Instant.now(), 
            Instant.now()
        );

        UUID correlationId = UUID.randomUUID();
        UUID parentId = null;
        TransactionType transactionType = TransactionType.DEPOSIT;

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId, transactionType, transactionMetadata, fees);

        portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, Instant.now());
    
        assertEquals(new Money(13999.93, cad), portfolio.getPortfolioCashBalance());
        assertEquals(1, portfolio.getTransactions().size());
    }

    @Test 
    void testAssetPurchase() {
        AssetIdentifier assetIdentifier;
        BigDecimal quantity;
        Money pricePerUnit;
        Money assetValueInAssetCurrency;
        Money assetValueInPortfolioCurrency;
        Money costBasisInPortfolioCurrency;
        Money totalFeesInPortfolioCurrency;

        assetIdentifier = new AssetIdentifier (
            "APPL",
            AssetType.STOCK,
            "US0378331005",
            "Apple", 
            "NASDAQ",
            "DESCRIPTION"
        );

        quantity = new BigDecimal(10);
        pricePerUnit = new Money(215.23, usd);
        assetValueInAssetCurrency = pricePerUnit.multiply(quantity);

        assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad, 
        new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "conversion")
        );

        totalFeesInPortfolioCurrency = new Money(0.0685, cad);

        costBasisInPortfolioCurrency = Money.of(
            quantity.multiply(pricePerUnit.amount()).add(totalFeesInPortfolioCurrency.amount()), 
            cad
        );

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricePerUnit, assetValueInAssetCurrency, assetValueInPortfolioCurrency, costBasisInPortfolioCurrency, totalFeesInPortfolioCurrency);

        // common transaction setup
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.BROKERAGE, new Money(0.05, usd))); // fees should have some way to link
        TransactionMetadata transactionMetadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "DEPOSITED MONEY", Instant.now(), 
            Instant.now()
        );

        UUID correlationId = UUID.randomUUID();
        UUID parentId = null;
        TransactionType transactionType = TransactionType.BUY;

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId, transactionType, transactionMetadata, fees);
        Instant transactionDate = Instant.now();

        portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate);

        assertEquals(new Money(9051.35-0.07, cad), portfolio.getPortfolioCashBalance());
    }

    @Test
    void testAssetPurchaseAssetExistsAlready() {
                AssetIdentifier assetIdentifier;
        BigDecimal quantity;
        Money pricePerUnit;
        Money assetValueInAssetCurrency;
        Money assetValueInPortfolioCurrency;
        Money costBasisInPortfolioCurrency;
        Money totalFeesInPortfolioCurrency;

        assetIdentifier = new AssetIdentifier (
            "APPL",
            AssetType.STOCK,
            "US0378331005",
            "Apple", 
            "NASDAQ",
            "DESCRIPTION"
        );

        quantity = new BigDecimal(10);
        pricePerUnit = new Money(215.23, usd);
        assetValueInAssetCurrency = pricePerUnit.multiply(quantity);

        assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad, 
        new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "conversion")
        );

        totalFeesInPortfolioCurrency = new Money(0.0685, cad);

        costBasisInPortfolioCurrency = Money.of(
            quantity.multiply(pricePerUnit.amount()).add(totalFeesInPortfolioCurrency.amount()), 
            cad
        );

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricePerUnit, assetValueInAssetCurrency, assetValueInPortfolioCurrency, costBasisInPortfolioCurrency, totalFeesInPortfolioCurrency);

        // common transaction setup
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.BROKERAGE, new Money(0.05, usd))); // fees should have some way to link
        TransactionMetadata transactionMetadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "DEPOSITED MONEY", Instant.now(), 
            Instant.now()
        );

        UUID correlationId = UUID.randomUUID();
        UUID parentId = null;
        TransactionType transactionType = TransactionType.BUY;

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId, transactionType, transactionMetadata, fees);
        Instant transactionDate = Instant.now();
        
        portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate);
        CommonTransactionInput commonTransactionInput2 = new CommonTransactionInput(UUID.randomUUID(), parentId, transactionType, transactionMetadata, fees);
        portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput2, transactionDate);

        assertEquals(2, portfolio.getTransactions().size());
        assertEquals(BigDecimal.valueOf(20), portfolio.getAssetHoldings().get(0).getTotalQuantity());
        assertEquals(new Money(4304.60, usd), portfolio.getAssetHoldings().get(0).getTotalAdjustedCostBasis());
    }

    @Test
    void testAssetPurchaseInValidCashBalanceNotEnough() {
        AssetIdentifier assetIdentifier;
        BigDecimal quantity;
        Money pricePerUnit;
        Money assetValueInAssetCurrency;
        Money assetValueInPortfolioCurrency;
        Money costBasisInPortfolioCurrency;
        Money totalFeesInPortfolioCurrency;

        assetIdentifier = new AssetIdentifier (
            "APPL",
            AssetType.STOCK,
            "US0378331005",
            "Apple", 
            "NASDAQ",
            "DESCRIPTION"
        );

        quantity = new BigDecimal(1000);
        pricePerUnit = new Money(215.23, usd);
        assetValueInAssetCurrency = pricePerUnit.multiply(quantity);

        assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad, 
        new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "conversion")
        );

        totalFeesInPortfolioCurrency = new Money(0.0685, cad);

        costBasisInPortfolioCurrency = Money.of(
            quantity.multiply(pricePerUnit.amount()).add(totalFeesInPortfolioCurrency.amount()), 
            cad
        );

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricePerUnit, assetValueInAssetCurrency, assetValueInPortfolioCurrency, costBasisInPortfolioCurrency, totalFeesInPortfolioCurrency);

        // common transaction setup
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.BROKERAGE, new Money(0.05, usd))); // fees should have some way to link
        TransactionMetadata transactionMetadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "DEPOSITED MONEY", Instant.now(), 
            Instant.now()
        );

        UUID correlationId = UUID.randomUUID();
        UUID parentId = null;
        TransactionType transactionType = TransactionType.BUY;

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId, transactionType, transactionMetadata, fees);
        Instant transactionDate = Instant.now();  
        
        assertThrows(InsufficientFundsException.class, () -> portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate));
    }

    @Test
    void testAssetPurchaseInValidNotBuyTransaction() {
        AssetIdentifier assetIdentifier;
        BigDecimal quantity;
        Money pricePerUnit;
        Money assetValueInAssetCurrency;
        Money assetValueInPortfolioCurrency;
        Money costBasisInPortfolioCurrency;
        Money costBasisInAssetCurrency;
        Money totalFeesInPortfolioCurrency;
        Money totalFeesInAssetCurrency;

        assetIdentifier = new AssetIdentifier (
            "APPL",
            AssetType.STOCK,
            "US0378331005",
            "Apple", 
            "NASDAQ",
            "DESCRIPTION"
        );

        quantity = new BigDecimal(1000);
        pricePerUnit = new Money(215.23, usd);
        assetValueInAssetCurrency = pricePerUnit.multiply(quantity);

        assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad, 
        new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "conversion")
        );

        totalFeesInPortfolioCurrency = new Money(0.0685, cad);
        totalFeesInAssetCurrency = new Money(0.0685*0.73, usd);

        // this is wrong
        costBasisInPortfolioCurrency = Money.of(
            quantity.multiply(pricePerUnit.amount()).add(totalFeesInPortfolioCurrency.amount()), 
            cad
        );

        costBasisInAssetCurrency = Money.of(
            quantity.multiply(pricePerUnit.amount().add(totalFeesInAssetCurrency.amount())
            ), usd);

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricePerUnit, assetValueInAssetCurrency, assetValueInPortfolioCurrency, costBasisInPortfolioCurrency, totalFeesInPortfolioCurrency);

        // common transaction setup
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.BROKERAGE, new Money(0.05, usd))); // fees should have some way to link
        TransactionMetadata transactionMetadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "DEPOSITED MONEY", Instant.now(), 
            Instant.now()
        );

        UUID correlationId = UUID.randomUUID();
        UUID parentId = null;
        TransactionType transactionType = TransactionType.SELL;

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId, transactionType, transactionMetadata, fees);
        Instant transactionDate = Instant.now();  

        Exception e1 = assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate));
        assertEquals("Expected BUY transaction type, got: " + commonTransactionInput.transactionType(), e1.getMessage());
    }
}
