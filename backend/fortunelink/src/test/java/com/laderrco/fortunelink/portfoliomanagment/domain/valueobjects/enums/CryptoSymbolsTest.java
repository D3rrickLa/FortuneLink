package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CryptoSymbolsTest {
    @Test 
    void test_IsCryptoCorrect() {
        assertTrue(CryptoSymbols.isCrypto("BTC"));
        assertTrue(CryptoSymbols.isCrypto("Btc"));
        assertTrue(CryptoSymbols.isCrypto("bTc"));
    }
    @Test 
    void test_IsCryptoInCorrect() {
        assertFalse(CryptoSymbols.isCrypto("BTCC"));
        assertFalse(CryptoSymbols.isCrypto("Btcc"));
        assertFalse(CryptoSymbols.isCrypto(""));
    }

}
