package com.laderrco.fortunelink.portfolio_management.application.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class TransactionResponseTest {
    @Test
    void testSetNetAmount() {
        ExchangeRate usdToCad = new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.CAD, BigDecimal.valueOf(1.35), Instant.now(), null);
        ExchangeRate cad = ExchangeRate.createSingle(ValidatedCurrency.CAD, null);
        List<Fee> fees = List.of(
            new Fee(FeeType.ANNUAL_FEE, Money.of(5, "USD"), usdToCad, null, Instant.now()),
            new Fee(FeeType.BACK_END_LOAD, Money.of(15, "USD"), usdToCad, null, Instant.now()),
            new Fee(FeeType.ANNUAL_FEE, Money.of(10, "CAD"), cad, null, Instant.now())
        );

        TransactionResponse response = new TransactionResponse(TransactionId.randomId(), TransactionType.DEPOSIT, "CAD", BigDecimal.ONE, Money.of(25, "CAD"), fees, Money.of(25, "CAD"), Instant.now(), null);
        Money actual = response.calculateNetAmount();
        assertEquals(Money.of(-12, "CAD"), actual);
    }
   
    @Test
    void testSetNetAmountNoFees() {
        TransactionResponse response = new TransactionResponse(TransactionId.randomId(), TransactionType.DEPOSIT, "CAD", BigDecimal.ONE, Money.of(25, "CAD"), null, Money.of(25, "CAD"), Instant.now(), null);
        Money actual = response.calculateNetAmount();
        assertEquals(Money.of(25, "CAD"), actual);
    }
    
    @Test
    void testEdgeCaseForNetAmount() {
        TransactionResponse response = new TransactionResponse(TransactionId.randomId(), TransactionType.DEPOSIT, "CAD", BigDecimal.ONE, Money.of(25, "CAD"), List.of(), Money.of(25, "CAD"), Instant.now(), null);
        Money actual = response.calculateNetAmount();
        assertEquals(Money.of(25, "CAD"), actual);

    }
}
