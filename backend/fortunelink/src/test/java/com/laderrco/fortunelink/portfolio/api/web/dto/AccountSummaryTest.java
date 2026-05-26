package com.laderrco.fortunelink.portfolio.api.web.dto;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountSummaryTest {

  @Test
  void shouldSuccessfullyMapFromAccountView() {
    // 1. Arrange: Set up mock data for AccountView and its nested objects
    UUID accountId = UUID.randomUUID();
    Instant now = now();

    // Mocking nested types based on the record's getter calls
    AccountView mockView = mock(AccountView.class);
    AccountType mockType = mock(AccountType.class);
    AccountLifecycleState mockStatus = mock(AccountLifecycleState.class);
    Currency mockCurrency = mock(Currency.class);
    Money mockCashBalance = mock(Money.class);
    Money mockTotalValue = mock(Money.class);
    List<?> mockAssets = mock(List.class);

    // Define behavior for mocks
    when(mockView.accountId()).thenReturn(new AccountId(accountId));
    when(mockView.name()).thenReturn("Premium Investment Account");
    when(mockView.type()).thenReturn(mockType);
    when(mockType.name()).thenReturn("INVESTMENT");
    when(mockView.status()).thenReturn(mockStatus);
    when(mockStatus.name()).thenReturn("ACTIVE");

    when(mockView.baseCurrency()).thenReturn(mockCurrency);
    when(mockCurrency.getCode()).thenReturn("USD");

    when(mockView.cashBalance()).thenReturn(mockCashBalance);
    when(mockCashBalance.amount()).thenReturn(new BigDecimal("5000.50"));

    when(mockView.totalValue()).thenReturn(mockTotalValue);
    when(mockTotalValue.amount()).thenReturn(new BigDecimal("125000.75"));

    when(mockView.assets()).thenReturn((List<PositionView>) mockAssets);
    when(mockAssets.size()).thenReturn(5);

    when(mockView.creationDate()).thenReturn(now);
    when(mockView.hasCashImbalance()).thenReturn(true);
    when(mockView.excludedTransactionCount()).thenReturn(3);

    // 2. Act: Call the method under test
    AccountSummary summary = AccountSummary.fromView(mockView);

    // 3. Assert: Verify all fields mapped exactly as expected
    assertNotNull(summary);
    assertEquals(accountId.toString(), summary.id());
    assertEquals("Premium Investment Account", summary.name());
    assertEquals("INVESTMENT", summary.type());
    assertEquals("ACTIVE", summary.status());
    assertEquals("USD", summary.currency());
    assertEquals(5000.50, summary.cashBalance());
    assertEquals(125000.75, summary.totalValue());
    assertEquals(5, summary.assetCount());
    assertEquals(now, summary.creationDate());
    assertTrue(summary.hasCashImbalance());
    assertEquals(3, summary.excludedTransactionCount());
  }

  @Test
  void shouldHandleEmptyOrZeroValuesGracefully() {
    // Arrange: Test edge cases like empty collections or zero amounts
    UUID accountId = UUID.randomUUID();
    Instant now = now();

    AccountView mockView = mock(AccountView.class);
    AccountType mockType = mock(AccountType.class);
    AccountLifecycleState mockStatus = mock(AccountLifecycleState.class);
    Currency mockCurrency = mock(Currency.class);
    Money mockMoney = mock(Money.class);

    when(mockView.accountId()).thenReturn(new AccountId(accountId));
    when(mockView.name()).thenReturn("");
    when(mockView.type()).thenReturn(mockType);
    when(mockType.name()).thenReturn("SAVINGS");
    when(mockView.status()).thenReturn(mockStatus);
    when(mockStatus.name()).thenReturn("PENDING");
    when(mockView.baseCurrency()).thenReturn(mockCurrency);
    when(mockCurrency.getCode()).thenReturn("EUR");

    when(mockView.cashBalance()).thenReturn(mockMoney);
    when(mockView.totalValue()).thenReturn(mockMoney);
    when(mockMoney.amount()).thenReturn(BigDecimal.ZERO);

    when(mockView.assets()).thenReturn(Collections.emptyList());
    when(mockView.creationDate()).thenReturn(now);
    when(mockView.hasCashImbalance()).thenReturn(false);
    when(mockView.excludedTransactionCount()).thenReturn(0);

    // Act
    AccountSummary summary = AccountSummary.fromView(mockView);

    // Assert
    assertEquals("", summary.name());
    assertEquals(0.0, summary.cashBalance());
    assertEquals(0.0, summary.totalValue());
    assertEquals(0, summary.assetCount());
    assertFalse(summary.hasCashImbalance());
    assertEquals(0, summary.excludedTransactionCount());
  }
}