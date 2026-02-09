package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.identifiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

public class AllIdentifierTest {
    @Test
    void testAllIdentifiersAccount_success() {
        AccountId accountId = AccountId.newId();
        AccountId fromId = AccountId.fromString(accountId.toString());
        assertEquals(accountId, fromId);
    }

    @Test
    void testAllIdentifiersPortfolio_success() {
        PortfolioId Id = PortfolioId.newId();
        PortfolioId fromId = PortfolioId.fromString(Id.toString());
        assertEquals(Id, fromId);
    }

    @Test
    void testAllIdentifiersTransaction_success() {
        TransactionId transactionId = TransactionId.newId();
        TransactionId fromId = TransactionId.fromString(transactionId.toString());
        assertEquals(transactionId, fromId);
    }

    @Test
    void testAllIdentifiersUser_success() {
        UserId userId = UserId.random();
        UserId fromId = UserId.fromString(userId.toString());
        assertEquals(userId, fromId);
    }

    @Test
    void testAssetSymbol_Success() {
        AssetSymbol symbol = new AssetSymbol("AAPL");
        assertEquals("AAPL", symbol.value());
    }

    @Test
    void testAssetSymbol_failure_symbolMustContainOnlyValid() {
        assertThrows(IllegalArgumentException.class, 
            () -> new AssetSymbol("AAPL%$#(")
        );
    }

    @Test
    void testAssetSymbol_failure_symbolTooLong() {
        assertThrows(IllegalArgumentException.class, 
            () -> new AssetSymbol("AAPLASDASDASDASDASDASDASDASDASDASDASDASDASDADS")
        );
    }
}
