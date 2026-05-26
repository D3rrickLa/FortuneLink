package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.views.RealizedGainView;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainsSummaryView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealizedGainsSummaryResponseTest {

  @Test
  void shouldSuccessfullyMapFromRealizedGainsSummaryView() {
    // 1. Arrange: Setup View, Summary Money mocks, and Currency
    RealizedGainsSummaryView mockView = mock(RealizedGainsSummaryView.class);
    Currency mockCurrency = mock(Currency.class);
    when(mockCurrency.getCode()).thenReturn("USD");
    when(mockView.currency()).thenReturn(mockCurrency);
    when(mockView.taxYear()).thenReturn(2026);

    // Mock summary total Money fields
    Money mockTotalGains = mockMoney("5000.00", mockCurrency);
    Money mockTotalLosses = mockMoney("1200.00", mockCurrency);
    Money mockNetGainLoss = mockMoney("3800.00", mockCurrency);

    when(mockView.totalGains()).thenReturn(mockTotalGains);
    when(mockView.totalLosses()).thenReturn(mockTotalLosses);
    when(mockView.netGainLoss()).thenReturn(mockNetGainLoss);

    // 2. Arrange Nested Items: Corrected to use RealizedGainView instead of the summary view
    Instant now = Instant.now();

    // Item A: A realization that resulted in a Gain
    RealizedGainView itemGain = mock(RealizedGainView.class);
    Money gainMoney = mockMoney("1500.00", mockCurrency);
    when(gainMoney.isNegative()).thenReturn(false);
    setupCommonItemViewExpectations(itemGain, "AAPL", gainMoney, "10000.00", mockCurrency, now,
        true);

    // Item B: A realization that resulted in a Loss (isGain = false, money is negative)
    RealizedGainView itemLoss = mock(RealizedGainView.class);
    Money lossMoney = mockMoney("-300.00", mockCurrency);
    when(lossMoney.isNegative()).thenReturn(true);
    setupCommonItemViewExpectations(itemLoss, "TSLA", lossMoney, "5000.00", mockCurrency, now,
        false);

    // Safely pass the correctly typed list to the mock view
    when(mockView.items()).thenReturn(List.of(itemGain, itemLoss));

    // 3. Act: Map to the summary DTO
    RealizedGainsSummaryResponse response = RealizedGainsSummaryResponse.from(mockView);

    // 4. Assert: Global structure and calculations
    assertNotNull(response);
    assertEquals("USD", response.currency());
    assertEquals(2026, response.taxYear());
    assertEquals(2, response.itemCount());
    assertEquals(2, response.items().size());

    // Assert mapped summary money objects
    assertEquals(new BigDecimal("5000.00"), response.totalGains().amount());
    assertEquals(new BigDecimal("1200.00"), response.totalLosses().amount());
    assertEquals(new BigDecimal("3800.00"), response.netGainLoss().amount());

    // Assert individual item mappings & inline flag logic
    RealizedGainItemResponse responseGainItem = response.items().get(0);
    assertEquals("AAPL", responseGainItem.symbol());
    assertTrue(responseGainItem.isGain());
    assertFalse(responseGainItem.isLoss(), "Should not be a loss if isGain is true");

    RealizedGainItemResponse responseLossItem = response.items().get(1);
    assertEquals("TSLA", responseLossItem.symbol());
    assertFalse(responseLossItem.isGain());
    assertTrue(responseLossItem.isLoss(), "Should be a loss if !isGain and money is negative");
  }

  @Test
  void shouldHandleEmptyCollectionsCleanly() {
    // Arrange: View contains no items
    RealizedGainsSummaryView mockView = mock(RealizedGainsSummaryView.class);
    Currency mockCurrency = mock(Currency.class);
    when(mockCurrency.getCode()).thenReturn("EUR");
    when(mockView.currency()).thenReturn(mockCurrency);
    when(mockView.taxYear()).thenReturn(2025);

    when(mockView.items()).thenReturn(Collections.emptyList());
    when(mockView.totalGains()).thenReturn(null);
    when(mockView.totalLosses()).thenReturn(null);
    when(mockView.netGainLoss()).thenReturn(null);

    // Act
    RealizedGainsSummaryResponse response = RealizedGainsSummaryResponse.from(mockView);

    // Assert
    assertNotNull(response);
    assertTrue(response.items().isEmpty());
    assertEquals(0, response.itemCount());
    assertNull(response.totalGains());
    assertNull(response.totalLosses());
    assertNull(response.netGainLoss());
  }

  // --- Private Helper Utilities for Clean Mock Setup ---

  private Money mockMoney(String amount, Currency currency) {
    Money mockMoney = mock(Money.class);
    when(mockMoney.amount()).thenReturn(new BigDecimal(amount));
    when(mockMoney.currency()).thenReturn(currency);
    return mockMoney;
  }

  // Corrected signature and body to cleanly target RealizedGainView properties directly
  private void setupCommonItemViewExpectations(RealizedGainView itemView, String symbol,
      Money gainLossMoney, String costBasisAmount, Currency currency, Instant occurredAt,
      boolean isGain) {
    when(itemView.symbol()).thenReturn(symbol);
    when(itemView.realizedGainLoss()).thenReturn(gainLossMoney);

    Money costBasisMoney = mockMoney(costBasisAmount, currency);
    when(itemView.costBasisSold()).thenReturn(costBasisMoney);

    when(itemView.occurredAt()).thenReturn(occurredAt);
    when(itemView.isGain()).thenReturn(isGain);
  }
}