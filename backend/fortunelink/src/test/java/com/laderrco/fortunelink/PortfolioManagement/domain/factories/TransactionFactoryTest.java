package com.laderrco.fortunelink.PortfolioManagement.domain.Factories;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagement.domain.factories.TransactionFactory;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class TransactionFactoryTest {

        @Test
        void testConstructor() {
                TransactionFactory transactionFactory = new TransactionFactory();
                assertNotNull(transactionFactory);
        }
        
        @Test
        void testCreateBuyAssetTransaction() {
                UUID transactionID = UUID.randomUUID();
                UUID portfolioID = UUID.randomUUID();
                AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
                Instant transactionDate = Instant.now();
                BigDecimal quantity = new BigDecimal(100);
                Money pricePerUnit = new Money(new BigDecimal(35.32), new PortfolioCurrency(Currency.getInstance("USD")));
                TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,
                        TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.PENDING,
                        TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                List<Fee> fees = new ArrayList<>();
                fees.add(new Fee(FeeType.COMMISSION,
                        new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should
                                                                                                        // be == to
                                                                                                        // portoflio
                                                                                                        // currency pref
                fees.add(new Fee(FeeType.BROKERAGE,
                        new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee
                                                                                                        // should be ==
                                                                                                        // to portoflio
                                                                                                        // currency pref

                Transaction transaction = TransactionFactory.createBuyAssetTransaction(transactionID, portfolioID,
                        assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees);
                Transaction transaction2 = TransactionFactory.createBuyAssetTransaction(transactionID, portfolioID,
                        assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata2, null);
                // failing tests
                assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(null,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        null, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, null, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, null, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, null, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, null, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, null, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, new BigDecimal(10), pricePerUnit, new TransactionMetadata(TransactionStatus.FAILED,
                        TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now()),
                        null));

                // quantity check
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, new BigDecimal(-6), pricePerUnit, transactionMetadata,
                        null));

                // price per unit check
                Money pricePerUnitNegative = new Money(new BigDecimal(-13.14),
                        new PortfolioCurrency(Currency.getInstance("USD")));
                Money pricePerUnitZero = new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")));

                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitNegative, transactionMetadata,
                        null));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitZero, transactionMetadata,
                        null));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitZero, transactionMetadata,
                        null));

                // final good test
                assertFalse(transaction.equals(null));
                assertFalse(transaction2.equals(null));
        }

        @Test
        void testCreateSellAssetTransaction() {
                UUID transactionID = UUID.randomUUID();
                UUID portfolioID = UUID.randomUUID();
                AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
                Instant transactionDate = Instant.now();
                BigDecimal quantity = new BigDecimal(100);
                Money pricePerUnit = new Money(new BigDecimal(35.32), new PortfolioCurrency(Currency.getInstance("USD")));
                TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.PENDING,TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                List<Fee> fees = new ArrayList<>();
                fees.add(new Fee(FeeType.COMMISSION,
                        new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should
                                                                                                        // be == to
                                                                                                        // portoflio
                                                                                                        // currency pref
                fees.add(new Fee(FeeType.BROKERAGE,
                        new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee
                                                                                                        // should be ==
                                                                                                        // to portoflio
                                                                                                        // currency pref

                Transaction transaction = TransactionFactory.createSellAssetTransaction(transactionID, portfolioID,
                        assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees);
                Transaction transaction2 = TransactionFactory.createSellAssetTransaction(transactionID, portfolioID,
                        assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata2, null);
                // failing tests
                assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(null,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        null, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, null, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, null, quantity, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, null, pricePerUnit, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, null, transactionMetadata, fees));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, null, fees));
                        
                // quantity check
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, new TransactionMetadata(TransactionStatus.CANCELLED, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now()), fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, new BigDecimal(-6), pricePerUnit, transactionMetadata,
                        null));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, new BigDecimal(0), pricePerUnit, transactionMetadata,
                        null));

                // price per unit check
                Money pricePerUnitNegative = new Money(new BigDecimal(-13.14),
                        new PortfolioCurrency(Currency.getInstance("USD")));
                Money pricePerUnitZero = new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")));

                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitNegative, transactionMetadata,
                        null));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                        portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitZero, transactionMetadata,
                        null));

                // final good test
                assertFalse(transaction.equals(null));
                assertFalse(transaction2.equals(null));

        }

        @Test
        void testCreateCashTransaction() {
                UUID transactionID = UUID.randomUUID();
                UUID portfolioID = UUID.randomUUID();
                TransactionType transactionType = TransactionType.DEPOSIT;
                Instant transactionDate = Instant.now();
                Money amount = new Money(new BigDecimal(350.32), new PortfolioCurrency(Currency.getInstance("USD")));
                TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.PENDING, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());

                List<Fee> fees = new ArrayList<>();
                fees.add(new Fee(FeeType.COMMISSION,
                        new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should
                                                                                                        // be == to
                                                                                                        // portoflio
                                                                                                        // currency pref
                fees.add(new Fee(FeeType.BROKERAGE,
                        new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee
                                                                                                                // should be ==
                                                                                                                // to portoflio
                                                                                                                // currency pref


                Transaction transaction1 = TransactionFactory.createCashTransaction(transactionID, portfolioID, transactionType, transactionDate, amount, transactionMetadata, fees);
                Transaction transaction2 = TransactionFactory.createCashTransaction(transactionID, portfolioID, TransactionType.EXPENSE, transactionDate, amount, transactionMetadata, fees);
                Transaction transaction3 = TransactionFactory.createCashTransaction(transactionID, portfolioID, TransactionType.EXPENSE, transactionDate, amount, transactionMetadata2, null);


                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCashTransaction(transactionID, portfolioID, TransactionType.EXPENSE, transactionDate, new Money(new BigDecimal(-1),  new PortfolioCurrency(Currency.getInstance("USD"))), transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCashTransaction(transactionID, portfolioID, TransactionType.EXPENSE, transactionDate, new Money(new BigDecimal(0),  new PortfolioCurrency(Currency.getInstance("USD"))), transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCashTransaction(transactionID, portfolioID, TransactionType.VOID_BUY, transactionDate, amount, transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCashTransaction(transactionID, portfolioID, TransactionType.REVERSE_STOCK_SPLIT, transactionDate, amount, transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCashTransaction(transactionID, portfolioID, TransactionType.REVERSE_STOCK_SPLIT, transactionDate, amount,  new TransactionMetadata(TransactionStatus.CANCELLED, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now()), fees));

                assertNotEquals(transaction2, transaction3);
                assertNotEquals(transaction1, transaction3);
    
        }

        @Test
        void testCreateVoidTransaction() {
            UUID transactionID = UUID.randomUUID();
            UUID portfolioID = UUID.randomUUID();
            UUID originalTransactionID = UUID.randomUUID();
            TransactionType transactionType = TransactionType.VOID_BUY;
            Money originalTransactionAmount = new Money(new BigDecimal(350.32), new PortfolioCurrency(Currency.getInstance("USD")));
            Instant transactionDate = Instant.now();
            String reason = "SOME REASON TO VOID";
        
            TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,
                    TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
        
            TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.PENDING,
                    TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
            
            Transaction transaction1 = TransactionFactory.createVoidTransaction(transactionID, portfolioID, originalTransactionID, transactionType, originalTransactionAmount, transactionDate, reason, transactionMetadata);
            Transaction transaction2 = TransactionFactory.createVoidTransaction(transactionID, portfolioID, originalTransactionID, transactionType, originalTransactionAmount, transactionDate, reason, transactionMetadata2);
        
            assertThrows(IllegalArgumentException.class, () ->  TransactionFactory.createVoidTransaction(transactionID, portfolioID, originalTransactionID, TransactionType.DEPOSIT, originalTransactionAmount, transactionDate, reason, transactionMetadata));
            assertThrows(IllegalArgumentException.class, () ->  TransactionFactory.createVoidTransaction(transactionID, portfolioID, originalTransactionID, transactionType, originalTransactionAmount, transactionDate, reason, new TransactionMetadata(TransactionStatus.CANCELLED, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now())));
            assertNotEquals(transaction1, transaction2);
        }

        @Test
        void testCreateAssetTransferInTransaction() {
                UUID transactionID = UUID.randomUUID();
                UUID sourcePortfolioID = UUID.randomUUID();
                UUID destinationPortfolioID = UUID.randomUUID();
                AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
                PortfolioCurrency portfolioCurrencyPref = new PortfolioCurrency(Currency.getInstance("USD"));
                Instant transactionDate = Instant.now();
                BigDecimal quantity = new BigDecimal(100);
                Money costBasisPerUnit = new Money(new BigDecimal(35.32), new PortfolioCurrency(Currency.getInstance("USD")));
                TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,
                        TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.PENDING,
                        TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                List<Fee> fees = new ArrayList<>();
                fees.add(new Fee(FeeType.COMMISSION,
                        new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should
                                                                                                        // be == to
                                                                                                        // portoflio
                                                                                                        // currency pref
                fees.add(new Fee(FeeType.BROKERAGE,
                        new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee
                                                                                                        // should be ==
                                                                                                        // to portoflio
                                                                                                        // currency pref
                Transaction transaction1 = TransactionFactory.createAssetTransferInTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, quantity, costBasisPerUnit, transactionMetadata, fees);
                Transaction transaction2 = TransactionFactory.createAssetTransferInTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, quantity, null, transactionMetadata2, null);
                

                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createAssetTransferInTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, new BigDecimal(-1), costBasisPerUnit, transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createAssetTransferInTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, new BigDecimal(0), costBasisPerUnit, transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createAssetTransferInTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, new BigDecimal(10), costBasisPerUnit,  new TransactionMetadata(TransactionStatus.CANCELLED, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now()), fees));
                assertNotEquals(transaction1, transaction2);
        }

        @Test
        void testCreateAssetTransferOutTransaction() {
                UUID transactionID = UUID.randomUUID();
                UUID sourcePortfolioID = UUID.randomUUID();
                UUID destinationPortfolioID = UUID.randomUUID();
                AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
                PortfolioCurrency portfolioCurrencyPref = new PortfolioCurrency(Currency.getInstance("USD"));
                Instant transactionDate = Instant.now();
                BigDecimal quantity = new BigDecimal(100);
                Money costBasisPerUnit = new Money(new BigDecimal(35.32), new PortfolioCurrency(Currency.getInstance("USD")));
                TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,
                TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.PENDING,
                TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                List<Fee> fees = new ArrayList<>();
                fees.add(new Fee(FeeType.COMMISSION,
                new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should
                // be == to
                // portoflio
                // currency pref
                fees.add(new Fee(FeeType.BROKERAGE,
                new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee
                // should be ==
                // to portoflio
                // currency pref
                Transaction transaction1 = TransactionFactory.createAssetTransferOutTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, quantity, costBasisPerUnit, transactionMetadata, fees);
                Transaction transaction2 = TransactionFactory.createAssetTransferOutTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, quantity, null, transactionMetadata2, null);
                
                
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createAssetTransferOutTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, new BigDecimal(-1), costBasisPerUnit, transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createAssetTransferOutTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, new BigDecimal(0), costBasisPerUnit, transactionMetadata, fees));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createAssetTransferOutTransaction(transactionID, destinationPortfolioID, sourcePortfolioID, assetIdentifier, portfolioCurrencyPref, transactionDate, new BigDecimal(10), costBasisPerUnit,  new TransactionMetadata(TransactionStatus.CANCELLED, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now()), fees));
                assertNotEquals(transaction1, transaction2);
        
        }


        @Test
        void testCreateCorporateActionTransaction() {
                UUID transactionID = UUID.randomUUID();
                UUID portfolioID = UUID.randomUUID();
                TransactionType transactionType = TransactionType.STOCK_SPLIT;
                PortfolioCurrency portfolioCurrencyPref = new PortfolioCurrency(Currency.getInstance("USD"));
                AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
                BigDecimal splitRatio = new BigDecimal("2");
                Instant transactionDate = Instant.now();
                TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());
                TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.PENDING,TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now());

                Transaction transaction1 = TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, transactionType, portfolioCurrencyPref, assetIdentifier, splitRatio, transactionDate, transactionMetadata);
                Transaction transaction2 = TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, TransactionType.CORPORATE_ACTION, portfolioCurrencyPref, assetIdentifier, null, transactionDate, transactionMetadata2);

                assertThrows(NullPointerException.class, () -> TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, transactionType, portfolioCurrencyPref, assetIdentifier, null, transactionDate, transactionMetadata));
                assertThrows(NullPointerException.class, () -> TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, TransactionType.REVERSE_STOCK_SPLIT, portfolioCurrencyPref, assetIdentifier, null, transactionDate, transactionMetadata));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, transactionType, portfolioCurrencyPref, assetIdentifier, new BigDecimal("-1"), transactionDate, transactionMetadata));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, transactionType, portfolioCurrencyPref, assetIdentifier, new BigDecimal("0"), transactionDate, transactionMetadata));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, TransactionType.CORPORATE_ACTION, portfolioCurrencyPref, assetIdentifier, splitRatio, transactionDate, transactionMetadata));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, TransactionType.BUY, portfolioCurrencyPref, assetIdentifier, splitRatio, transactionDate, transactionMetadata));
                assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createCorporateActionTransaction(transactionID, portfolioID, transactionType, portfolioCurrencyPref, assetIdentifier, splitRatio, transactionDate,  new TransactionMetadata(TransactionStatus.CANCELLED, TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION", Instant.now(), Instant.now())));

                assertNotEquals(transaction1, transaction2);
        }


}
