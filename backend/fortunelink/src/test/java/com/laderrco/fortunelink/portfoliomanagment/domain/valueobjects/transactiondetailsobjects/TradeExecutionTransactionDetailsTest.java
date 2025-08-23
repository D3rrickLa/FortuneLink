// package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

// import static org.junit.jupiter.api.Assertions.assertEquals;

// import java.math.BigDecimal;
// import java.time.Instant;
// import java.util.Currency;
// import java.util.List;
// import java.util.UUID;

// import org.junit.jupiter.api.Test;

// import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
// import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
// import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
// import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
// import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
// import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
// import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
// import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
// import com.laderrco.fortunelink.portfoliomanagment.infrastructure.services.SimpleCurrencyService;


// public class TradeExecutionTransactionDetailsTest {
//     private final Currency nativeCurrency = Currency.getInstance("CAD");
//     private final Currency portfolioCurrency = Currency.getInstance("USD");

//     private final CurrencyConversionService exchangeRateService = new SimpleCurrencyService();


//     @Test
//     void testConstructionAndFieldValues() {
//         AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "AAPL", "US0378331005", "Apple", "NASDAQ", Currency.getInstance("USD"));
//         BigDecimal quantity = new BigDecimal("10");
//         Money pricePerUnit = new Money(new BigDecimal("100"), nativeCurrency);
//         Fee fee1 = new Fee(FeeType.TRANSACTION_FEE, new Money(new BigDecimal("5"), nativeCurrency), "Transaction Fee", Instant.now());
//         Fee fee2 = new Fee(FeeType.ACCOUNT_MAINTENANCE, new Money(new BigDecimal("2.5"), nativeCurrency), "Platform Fee", Instant.now());
//         List<Fee> nativeFees = List.of(fee1, fee2);
//         AssetHoldingId assetHoldingId = new AssetHoldingId(UUID.randomUUID());

//         TradeExecutionTransactionDetails details = TradeExecutionTransactionDetails.createBuyDetails(
//                 assetIdentifier,
//                 quantity,
//                 pricePerUnit,
//                 TransactionSource.MANUAL,
//                 "Purchase of 10 shares of AAPL",
//                 nativeFees,
//                 portfolioCurrency,
//                 assetHoldingId
//         );

//         // Assert basic fields
//         assertEquals(assetIdentifier, details.getAssetIdentifier());
//         assertEquals(quantity, details.getQuantity());
//         assertEquals(pricePerUnit, details.getPricePerUnit());
//         assertEquals(assetHoldingId, details.getAssetHoldingId());

//         // Asset value in native
//         Money expectedNativeValue = pricePerUnit.multiply(quantity);
//         assertEquals(expectedNativeValue, details.getAssetValueInNativeCurrency());

//         // Asset value in portfolio currency
//         Money convertedPPU = exchangeRateService.convert(pricePerUnit, portfolioCurrency);
//         Money expectedPortfolioValue = convertedPPU.multiply(quantity);
//         assertEquals(expectedPortfolioValue, details.getAssetValueInPortfolioCurrency());

//         // Total fees converted
//         Money convertedFee1 = exchangeRateService.convert(fee1.amount(), portfolioCurrency);
//         Money convertedFee2 = exchangeRateService.convert(fee2.amount(), portfolioCurrency);
//         Money expectedTotalFees = convertedFee1.add(convertedFee2);
//         assertEquals(expectedTotalFees, details.getTotalFeesInPortfolioCurrency());
//     }

//     @Test
//     void testZeroFeesHandledCorrectly() {
//         AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "GOOG", "US02079K3059", "Google", "NASDAQ", Currency.getInstance("USD"));
//         BigDecimal quantity = new BigDecimal("5");
//         Money pricePerUnit = new Money(new BigDecimal("200"), nativeCurrency);
//         List<Fee> nativeFees = List.of(); // No fees
//         AssetHoldingId assetHoldingId = new AssetHoldingId(UUID.randomUUID());

//         TradeExecutionTransactionDetails details = new TradeExecutionTransactionDetails(
//                 assetIdentifier,
//                 quantity,
//                 pricePerUnit,
//                 TransactionSource.MANUAL,
//                 "Purchase of 5 shares of GOOG",
//                 nativeFees,
//                 portfolioCurrency,
//                 assetHoldingId,
//                 exchangeRateService
//         );

//         Money expectedTotalFees = Money.ZERO(portfolioCurrency);
//         assertEquals(expectedTotalFees, details.getTotalFeesInPortfolioCurrency());
//     }

//     @Test
//     void testSameCurrencyConversion() {
//         Currency sameCurrency = Currency.getInstance("CAD");

//         AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "TSLA", "US88160R1014", "Tesla", "NASDAQ", Currency.getInstance("USD"));
//         BigDecimal quantity = new BigDecimal("3");
//         Money pricePerUnit = new Money(new BigDecimal("150"), sameCurrency);
//         List<Fee> fees = List.of(new Fee(FeeType.OTHER, new Money(new BigDecimal("10"), sameCurrency), "Flat Fee", Instant.now()));
//         AssetHoldingId assetHoldingId = new AssetHoldingId(UUID.randomUUID());

//         TradeExecutionTransactionDetails details = new TradeExecutionTransactionDetails(
//                 assetIdentifier,
//                 quantity,
//                 pricePerUnit,
//                 TransactionSource.SYSTEM,
//                 "Tesla trade",
//                 fees,
//                 sameCurrency,
//                 assetHoldingId,
//                 exchangeRateService
//         );

//         // Should not convert anything — currencies match
//         Money expectedAssetValue = pricePerUnit.multiply(quantity);
//         assertEquals(expectedAssetValue, details.getAssetValueInPortfolioCurrency());

//         Money expectedTotalFees = fees.get(0).amount(); // No conversion
//         assertEquals(expectedTotalFees, details.getTotalFeesInPortfolioCurrency());
//     }

// }
