package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RealizedGainItemResponseTest {

  @Test
  void shouldSuccessfullyMapFromRealizedGainRecord() {
    // 1. Arrange: Mock the root record and all nested domain dependencies
    RealizedGainRecord mockRecord = mock(RealizedGainRecord.class);

    // Mock for symbol nested object
    AssetSymbol mockSymbolObj = mock(AssetSymbol.class);
    when(mockSymbolObj.symbol()).thenReturn("AAPL");
    when(mockRecord.symbol()).thenReturn(mockSymbolObj);

    // Mock for realizedGainLoss Money object
    Money mockGainLossMoney = mock(Money.class);
    Currency usd = mock(Currency.class);
    when(usd.getCode()).thenReturn("USD");
    when(mockGainLossMoney.amount()).thenReturn(new BigDecimal("1500.00"));
    when(mockGainLossMoney.currency()).thenReturn(usd);
    when(mockRecord.realizedGainLoss()).thenReturn(mockGainLossMoney);

    // Mock for costBasisSold Money object
    Money mockCostBasisMoney = mock(Money.class);
    when(mockCostBasisMoney.amount()).thenReturn(new BigDecimal("10000.00"));
    when(mockCostBasisMoney.currency()).thenReturn(usd);
    when(mockRecord.costBasisSold()).thenReturn(mockCostBasisMoney);

    // Mock primitives and simple objects
    Instant now = Instant.now();
    when(mockRecord.occurredAt()).thenReturn(now);
    when(mockRecord.isGain()).thenReturn(true);
    when(mockRecord.isLoss()).thenReturn(false);

    // 2. Act: Map to the DTO
    RealizedGainItemResponse response = RealizedGainItemResponse.from(mockRecord);

    // 3. Assert: Verify the outer mappings and the nested MoneyResponse properties
    assertNotNull(response);
    assertEquals("AAPL", response.symbol());
    assertEquals(now, response.occurredAt());
    assertTrue(response.isGain());
    assertFalse(response.isLoss());

    // Verify nested MoneyResponse objects were mapped correctly
    assertNotNull(response.realizedGainLoss());
    assertEquals(new BigDecimal("1500.00"), response.realizedGainLoss().amount());
    assertEquals("USD", response.realizedGainLoss().currency());

    assertNotNull(response.costBasisSold());
    assertEquals(new BigDecimal("10000.00"), response.costBasisSold().amount());
    assertEquals("USD", response.costBasisSold().currency());
  }

  @Test
  void shouldHandleNullMoneyFieldsGracefully() {
    // Arrange: Set up a record where the Money objects might be null
    RealizedGainRecord mockRecord = mock(RealizedGainRecord.class);
    AssetSymbol mockSymbolObj = mock(AssetSymbol.class);

    when(mockSymbolObj.symbol()).thenReturn("MSFT");
    when(mockRecord.symbol()).thenReturn(mockSymbolObj);

    // Simulating null domain values for money fields
    when(mockRecord.realizedGainLoss()).thenReturn(null);
    when(mockRecord.costBasisSold()).thenReturn(null);

    when(mockRecord.occurredAt()).thenReturn(Instant.now());
    when(mockRecord.isGain()).thenReturn(false);
    when(mockRecord.isLoss()).thenReturn(false);

    // Act
    RealizedGainItemResponse response = RealizedGainItemResponse.from(mockRecord);

    // Assert: Ensure the DTO handles null nested objects safely thanks to MoneyResponse's null-guard
    assertNotNull(response);
    assertEquals("MSFT", response.symbol());
    assertNull(response.realizedGainLoss());
    assertNull(response.costBasisSold());
  }
}