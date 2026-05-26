package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyResponseTest {

  @Test
  void shouldSuccessfullyMapFromMoney() {
    // 1. Arrange: Mock the Money domain object and its nested Currency object
    Money mockMoney = mock(Money.class);
    Currency mockCurrency = mock(Currency.class);
    BigDecimal expectedAmount = new BigDecimal("1250.75");

    when(mockMoney.amount()).thenReturn(expectedAmount);
    when(mockMoney.currency()).thenReturn(mockCurrency);
    when(mockCurrency.getCode()).thenReturn("USD");

    // 2. Act: Map to the DTO
    MoneyResponse response = MoneyResponse.from(mockMoney);

    // 3. Assert: Verify values match perfectly
    assertNotNull(response);
    assertEquals(expectedAmount, response.amount());
    assertEquals("USD", response.currency());
  }

  @Test
  void shouldReturnNullWhenMoneyIsNull() {
    // Act: Pass a null reference into the factory method
    MoneyResponse response = MoneyResponse.from(null);

    // Assert: Ensure it cleanly returns null without throwing a NullPointerException
    assertNull(response);
  }

  @Test
  void shouldCreateZeroResponseWithGivenCurrency() {
    // Act: Request a zeroed-out instance for a specific currency
    String targetCurrency = "EUR";
    MoneyResponse response = MoneyResponse.zero(targetCurrency);

    // Assert: Verify it defaults to BigDecimal.ZERO and preserves the currency code
    assertNotNull(response);
    assertEquals(BigDecimal.ZERO, response.amount());
    assertEquals("EUR", response.currency());
  }
}