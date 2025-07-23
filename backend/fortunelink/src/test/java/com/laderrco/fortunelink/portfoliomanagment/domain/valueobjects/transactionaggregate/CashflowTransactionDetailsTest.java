package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;

public class CashflowTransactionDetailsTest {
    private Money originalCashflowAmount;
    private Money convertedCashflowAmount;
    private Money totalConversionFees;
    private ExchangeRate exchangeRate;
    private Currency cad;
    private Currency usd;

    @BeforeEach
    void init() {
        cad = Currency.getInstance("CAD");
        usd = Currency.getInstance("USD");
        originalCashflowAmount = new Money(1200.45, cad);
        convertedCashflowAmount = new Money(1200.45 * 0.73, usd);
        totalConversionFees = new Money(0.05, cad);
        exchangeRate = new ExchangeRate(cad, 
            usd, 
            BigDecimal.valueOf(1.37), 
            Instant.now(), 
            "YAHOO"
        );


    }
    
    @Test
    void testConstructor() {
        CashflowTransactionDetails transactionDetails = new CashflowTransactionDetails(originalCashflowAmount, convertedCashflowAmount, totalConversionFees, exchangeRate);
        assertNotNull(transactionDetails);

        assertEquals(originalCashflowAmount, transactionDetails.getOriginalCashflowAmount());
        assertEquals(convertedCashflowAmount, transactionDetails.getConvertedCashflowAmount());
        assertEquals(totalConversionFees, transactionDetails.getTotalConversionFees());
        assertEquals(exchangeRate, transactionDetails.getExchangeRate());
    }

    @Test
    void testConstructorInValidNulls() {
        
    }
}
