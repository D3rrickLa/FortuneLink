package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public class CashIdentifierTest {
    private CashIdentifier testIdentifier;
    private String Id;
    private ValidatedCurrency currency;

    @BeforeEach
    void init() {
        Id = "CAD";
        currency = ValidatedCurrency.CAD;
        testIdentifier = new CashIdentifier(Id, currency);
    }

    @Test 
    void testConstructor_SingleVar_Sucess() {
        CashIdentifier cashIdentifier = new CashIdentifier("CAD");
        assertEquals("CAD", cashIdentifier.getPrimaryId());;
        assertEquals("CASH: CAD", cashIdentifier.displayName());
        assertEquals(AssetType.CASH, cashIdentifier.getAssetType());
    }

    @Test
    void testConstructor_Success() {
        assertEquals("CAD", testIdentifier.getPrimaryId());;
        assertEquals("CASH: CAD", testIdentifier.displayName());
        assertEquals(AssetType.CASH, testIdentifier.getAssetType());
    }

    @Test
    void testConstructor_FailsWhenNullPass() {
        assertThrows(NullPointerException.class, () -> {
           new CashIdentifier(null, currency);
           new CashIdentifier(Id, null);
        });
    }
}
