package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.description;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CurrencyConversion;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;

public class TransactionDetailsTest {
    private List<Fee> fees;
    private Currency CAD = Currency.getInstance("CAD");
    private Currency USD = Currency.getInstance("USD");
    private Currency EUR = Currency.getInstance("EUR");
    private CurrencyConversion usdToCad = CurrencyConversion.of("USD", "CAD", 1.38, Instant.now());
    private CurrencyConversion eurToCad = CurrencyConversion.of("EUR", "CAD", 1.60, Instant.now());
    @BeforeEach
    void init() {
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
                    .type(FeeType.FOREIGN_EXCHANGE_CONVERSION)
                    .amount(MonetaryAmount.of(Money.of(7.56, EUR), eurToCad))
                    .description("fx stuff")
                    .time(Instant.now())
                    .build();
        fees = new ArrayList<Fee>();
        fees.add(fee01);
        fees.add(fee02);
        fees.add(fee03);
    }

    @Test
    void testGetTotalFeesInCurrency() {
        TransactionSource source = TransactionSource.MANUAL;
        String description = "some description";

        TransactionDetails transactionDetails = new TransactionDetails(source, description, fees) {
            
        };
        assertDoesNotThrow(() ->transactionDetails.getTotalFeesInCurrency(CAD));
    }

    @Test
    void testGetTotalFeesInCurrency_ThrowExceptionWhenCurrencyDoesNotMatch() {
        TransactionSource source = TransactionSource.MANUAL;
        String description = "some description";

        TransactionDetails transactionDetails = new TransactionDetails(source, description, fees) {
            
        };
        assertThrows(CurrencyMismatchException.class, () ->transactionDetails.getTotalFeesInCurrency(EUR));
    }

    @Test
    void testGetters() {
        TransactionSource source = TransactionSource.MANUAL;
        String description = "some description";

        TransactionDetails transactionDetails = new TransactionDetails(source, description, fees) {
            
        };
        assertDoesNotThrow(() ->transactionDetails.getTotalFeesInCurrency(CAD));
        assertEquals(source, transactionDetails.getSource());
        assertEquals(description, transactionDetails.getDescription());
        assertEquals(fees, transactionDetails.getFees());
    }
}
