package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

public class DividendReceivedEventTest {

    @Test 
    void testDividendRecievedEventCorrect() {
        AssetIdentifier identifier = new AssetIdentifier(
            AssetType.STOCK,
            "AAPL",
            "US1234567890",
            "Apple Inc.",
            "NASDAQ"
        );

        Money amount = Money.of(20, Currency.getInstance("USD"));
        assertDoesNotThrow(() -> new DividendReceivedEvent(identifier, amount, Instant.now()));
    }
}
