package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordTransactionRequestTest {

    @Test
    @DisplayName("Constructor and getters/setters work correctly")
    void constructor_and_getters() {
        LocalDateTime now = LocalDateTime.now();
        RecordTransactionRequest req = new RecordTransactionRequest(
                "BUY",
                "AAPL",
                BigDecimal.TEN,
                BigDecimal.valueOf(100),
                "USD",
                List.of(),
                now,
                "Notes",
                true,
                BigDecimal.valueOf(5)
        );

        assertEquals("BUY", req.getTransactionType());
        assertEquals("AAPL", req.getSymbol());
        assertEquals(BigDecimal.TEN, req.getQuantity());
        assertEquals(BigDecimal.valueOf(100), req.getPrice());
        assertEquals("USD", req.getPriceCurrency());
        assertEquals(now, req.getTransactionDate());
        assertEquals("Notes", req.getNotes());
        assertTrue(req.getIsDrip());
        assertEquals(BigDecimal.valueOf(5), req.getSharesReceived());
    }

    @Test
    @DisplayName("isAssetTransaction returns true for BUY, SELL, DIVIDEND, INTEREST, FEE")
    void isAssetTransaction_returnsTrue() {
        String[] assetTypes = {"BUY", "SELL", "DIVIDEND", "INTEREST", "FEE"};
        for (String type : assetTypes) {
            RecordTransactionRequest req = new RecordTransactionRequest();
            req.setTransactionType(type);
            assertTrue(req.isAssetTransaction(), "Expected true for type: " + type);
        }
    }

    @Test
    @DisplayName("isAssetTransaction returns false for other types")
    void isAssetTransaction_returnsFalse() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setTransactionType("DEPOSIT");
        assertFalse(req.isAssetTransaction());

        req.setTransactionType("WITHDRAWAL");
        assertFalse(req.isAssetTransaction());

        req.setTransactionType(null);
        assertFalse(req.isAssetTransaction());
    }

    @Test
    @DisplayName("isCashTransaction returns true for DEPOSIT, WITHDRAWAL")
    void isCashTransaction_returnsTrue() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setTransactionType("DEPOSIT");
        assertTrue(req.isCashTransaction());

        req.setTransactionType("WITHDRAWAL");
        assertTrue(req.isCashTransaction());
    }

    @Test
    @DisplayName("isCashTransaction returns false for other types")
    void isCashTransaction_returnsFalse() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setTransactionType("BUY");
        assertFalse(req.isCashTransaction());

        req.setTransactionType("SELL");
        assertFalse(req.isCashTransaction());

        req.setTransactionType(null);
        assertFalse(req.isCashTransaction());
    }

    @Test
    @DisplayName("validateFields passes for valid asset transaction")
    void validateFields_validAsset() {
        RecordTransactionRequest req = new RecordTransactionRequest(
                "BUY",
                "AAPL",
                BigDecimal.TEN,
                BigDecimal.valueOf(100),
                "USD",
                List.of(),
                LocalDateTime.now(),
                "Notes",
                null,
                null
        );

        assertDoesNotThrow(req::validateFields);
    }

    @Test
    @DisplayName("validateFields passes for valid cash transaction")
    void validateFields_validCash() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setTransactionType("DEPOSIT");
        req.setQuantity(BigDecimal.valueOf(500));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());

        assertDoesNotThrow(req::validateFields);
    }

    @Test
    @DisplayName("validateFields throws exception for missing asset fields")
    void validateFields_missingAssetFields() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setTransactionType("BUY");
        req.setTransactionDate(LocalDateTime.now());

        Exception e = assertThrows(NullPointerException.class, req::validateFields);
        assertTrue(e.getMessage().contains("Asset symbol is required") || e.getMessage().contains("Quantity is required"));
    }

    @Test
    @DisplayName("validateFields throws exception for missing cash fields")
    void validateFields_missingCashFields() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setTransactionType("DEPOSIT");
        req.setTransactionDate(LocalDateTime.now());

        Exception e = assertThrows(NullPointerException.class, req::validateFields);
        assertTrue(e.getMessage().contains("Amount is required") || e.getMessage().contains("Currency is required"));
    }

    @Test
    @DisplayName("validateFields throws exception for missing transaction date")
    void validateFields_missingTransactionDate() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setTransactionType("BUY");
        req.setSymbol("AAPL");
        req.setQuantity(BigDecimal.ONE);
        req.setPrice(BigDecimal.ONE);
        req.setPriceCurrency("USD");

        Exception e = assertThrows(NullPointerException.class, req::validateFields);
        assertEquals("Transaction date is required", e.getMessage());
    }
}
