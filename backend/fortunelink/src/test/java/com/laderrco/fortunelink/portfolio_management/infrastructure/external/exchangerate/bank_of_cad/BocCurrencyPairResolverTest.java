package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class BocCurrencyPairResolverTest {

    @Test
    void resolveSeries_ShouldReturnDirectPair_WhenCADIsBase() {
        // CAD to USD -> FXCADUSD
        List<String> result = BocCurrencyPairResolver.resolveSeries("CAD", "USD");
        
        assertEquals(1, result.size());
        assertEquals("FXCADUSD", result.get(0));
    }

    @Test
    void resolveSeries_ShouldReturnDirectPair_WhenCADIsTarget() {
        // USD to CAD -> FXUSDCAD
        List<String> result = BocCurrencyPairResolver.resolveSeries("USD", "CAD");
        
        assertEquals(1, result.size());
        assertEquals("FXUSDCAD", result.get(0));
    }

    @Test
    void resolveSeries_ShouldReturnTwoSeries_WhenCrossCurrencyViaCAD() {
        // EUR to USD -> [FXEURCAD, FXCADUSD]
        List<String> result = BocCurrencyPairResolver.resolveSeries("EUR", "USD");
        
        assertEquals(2, result.size());
        assertEquals("FXEURCAD", result.get(0));
        assertEquals("FXCADUSD", result.get(1));
    }

    @Test
    void resolveSeries_ShouldHandleLowerCaseInputs() {
        List<String> result = BocCurrencyPairResolver.resolveSeries("eur", "cad");
        assertEquals("FXEURCAD", result.get(0));
    }

    @Test
    void resolveSeries_ShouldThrowException_WhenCurrenciesAreSame() {
        assertThrows(IllegalArgumentException.class, () -> {
            BocCurrencyPairResolver.resolveSeries("USD", "USD");
        });
    }
}