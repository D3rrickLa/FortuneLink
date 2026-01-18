package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;

public class SymbolIdentifierTest {
    @Test
    void testConstructor() {
        SymbolIdentifier identifier = SymbolIdentifier.of("AAPL");
        assertAll(
            () -> assertEquals("AAPL", identifier.getPrimaryId()),
            () -> assertEquals("UNKNOWN, SYMBOL GIVEN ONLY", identifier.displayName()),
            () -> assertEquals(AssetType.OTHER, identifier.getAssetType())
        );
    }
}
