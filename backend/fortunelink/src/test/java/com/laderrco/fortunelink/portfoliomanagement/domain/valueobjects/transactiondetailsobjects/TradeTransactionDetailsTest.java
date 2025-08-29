package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.IncomeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidPriceException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CurrencyConversion;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;

public class TradeTransactionDetailsTest {
    private AssetHoldingId assetHoldingId;
    private AssetIdentifier assetIdentifier;

    CurrencyConversion usdToCad;
    private static final String VALID_ISIN = "US0378331005"; // Apple ISIN
    private BigDecimal quantity;
    private MonetaryAmount pricePerUnit;
    private Currency CAD = Currency.getInstance("CAD");
    private Currency USD = Currency.getInstance("USD");

    private TransactionSource testSource;
    private String description;
    private List<Fee> testFees;
    
    @BeforeEach
    void init() {
        assetHoldingId = AssetHoldingId.createRandom();
        assetIdentifier = new AssetIdentifier(
                AssetType.STOCK,
                VALID_ISIN,
                Map.of("CUSIP", "037833100"),
                "Apple Inc.",
                "NASDAQ",
                "USD"
        );
        quantity = BigDecimal.valueOf(3500);

        usdToCad = new CurrencyConversion(USD, CAD, BigDecimal.valueOf(1.35));

        pricePerUnit = MonetaryAmount.of(Money.of(213.46, USD), usdToCad);

        testSource = TransactionSource.MANUAL;
        description = "something here";
        testFees = List.of(mock(Fee.class));

    }
    
    @Test
    void constructorShouldBeValid() {
        TradeTransactionDetails details = new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, null, null, CAD, testSource, description, testFees);

        
        assertEquals(assetHoldingId, details.getAssetHoldingId());
        assertEquals(assetIdentifier, details.getAssetIdentifier());
        assertEquals(quantity, details.getQuantity());
        assertEquals(pricePerUnit, details.getPricePerUnit());
        assertNull(details.getRealizedGainLoss());
        assertNull(details.getAcbPerUnitAtSale());
        assertEquals(CAD, details.getPortfolioCurrency());
    }
    
    @Test
    void constructorShouldThrowErrorWhenQuantityIs0OrLess() {
        BigDecimal negativeQuantity = BigDecimal.valueOf(-1);
        
        assertThrows(IllegalArgumentException.class, ()->new TradeTransactionDetails(assetHoldingId, assetIdentifier, negativeQuantity, pricePerUnit, null, null, CAD, testSource, description, testFees));
        BigDecimal zeroQuantity = BigDecimal.valueOf(0);
        assertThrows(IllegalArgumentException.class, ()->new TradeTransactionDetails(assetHoldingId, assetIdentifier, zeroQuantity, pricePerUnit, null, null, CAD, testSource, description, testFees));
    }
    
    @Test
    void constructorShouldThrowErrorWhenPriceIsNegative() {
        MonetaryAmount pricePerUnitNegative = MonetaryAmount.of(Money.of(-1, USD), usdToCad);
        assertThrows(InvalidPriceException.class, ()->new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnitNegative, null, null, CAD, testSource, description, testFees));
    }

    @Test
    void testBuy() {
        assertDoesNotThrow(() -> TradeTransactionDetails.buy(assetHoldingId, assetIdentifier, quantity, pricePerUnit, CAD, testSource, description, testFees));
    }

    @Test
    void testSell() {
        MonetaryAmount realizedGainLoss = MonetaryAmount.of(Money.of(50, "USD"), usdToCad);
        MonetaryAmount  acbPerUniAmount = MonetaryAmount.of(Money.of(2400, "USD"), usdToCad);

        assertDoesNotThrow(()-> TradeTransactionDetails.sell(assetHoldingId, assetIdentifier, quantity, acbPerUniAmount, realizedGainLoss, acbPerUniAmount, CAD, testSource, description, testFees));
    }

    @Test
    void grossValueGettters() {
        TradeTransactionDetails details = new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, null, null, CAD, testSource, description, testFees);
        assertEquals(MonetaryAmount.of(Money.of(747110, USD), usdToCad), details.getGrossValue());
        assertEquals(MonetaryAmount.of(Money.of(1008598.5, CAD), CurrencyConversion.identity(CAD)).nativeAmount(), details.getGrossValueInPortfolioCurrency());
    }

    @Test
    void calcaulteNetImpact() {
        List<Fee> newFees = new ArrayList<>();
        Fee fee01 = Fee.builder()
                    .type(FeeType.COMMISSION)
                    .amount(MonetaryAmount.of(Money.of(5, USD), usdToCad))
                    .description("commission")
                    .time(Instant.now())
                    .build();
        Fee fee02 = Fee.builder()
                    .type(FeeType.ANNUAL_FEE)
                    .amount(MonetaryAmount.of(Money.of(56, USD), usdToCad))
                    .description("annual fee")
                    .time(Instant.now())
                    .build();
        Fee fee03 = Fee.builder()
                    .type(FeeType.BROKERAGE)
                    .amount(MonetaryAmount.of(Money.of(7.56, CAD), CurrencyConversion.identity("CAD")))
                    .description("stuff")
                    .time(Instant.now())
                    .build();

        newFees.add(fee01);
        newFees.add(fee02);
        newFees.add(fee03);
        // total fees in CAD is 89.91
        TradeTransactionDetails details = new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, null, null, CAD, testSource, description, newFees);
        
        assertEquals(Money.of(1008688.41, "CAD"), details.calculateNetImpact(TradeType.BUY));
        assertEquals(Money.of(1008598.5-89.91, "CAD"), details.calculateNetImpact(TradeType.SELL));
        assertEquals(Money.of(0, "CAD"), details.calculateNetImpact(TradeType.OPTIONS_EXPIRED));
        assertEquals(Money.of(0, "CAD"), details.calculateNetImpact(TradeType.OTHER_TRADE_TYPE_REVERSAL));
        assertThrows(UnsupportedOperationException.class, () -> details.calculateNetImpact(TradeType.CRYPTO_SWAP));
        assertThrows(IllegalArgumentException.class, () -> details.calculateNetImpact(IncomeType.BONUS));
    }
    
    @Test
    void IsBuyOrSell() {
        TradeTransactionDetails details = new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, null, null, CAD, testSource, description, testFees);
        assertFalse(details.isBuy(TradeType.COVER_SHORT));
        assertFalse(details.isSell(TradeType.COVER_SHORT));
        assertTrue(details.isBuy(TradeType.BUY));
        assertTrue(details.isSell(TradeType.SELL));
    }
}
