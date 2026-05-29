package com.laderrco.fortunelink.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;

@ExtendWith(MockitoExtension.class)
@Profile("Test")
class ValuationApplicationServiceTest {

  private final UserId userId = UserId.fromString(UUID.randomUUID().toString());
  @Mock
  private PortfolioLoader portfolioLoader;
  @Mock
  private PortfolioValuationService portfolioValuationService;
  @Mock
  private UserPreferencesService userPreferencesService;
  @InjectMocks
  private ValuationApplicationService service;

  @Test
  @DisplayName("Summary valuation uses user preference base currency")
  void summaryUsesUserBaseCurrency() {
    Portfolio p1 = mock(Portfolio.class);
    when(portfolioLoader.loadAllUserPortfolios(userId)).thenReturn(List.of(p1));

    UserPreferences prefs = mock(UserPreferences.class);
    when(prefs.getBaseCurrency()).thenReturn(Currency.CAD);

    when(userPreferencesService.get(userId)).thenReturn(prefs);

    // Act
    service.computeSummaryValuation(userId);

    // Assert
    verify(portfolioValuationService).calculatePortfolioValuation(eq(p1), eq(Currency.CAD),
        anyMap());
  }

  @Test
  @DisplayName("Individual portfolio valuation uses portfolio's own display currency")
  void individualUsesPortfolioCurrency() {
    PortfolioId pid = PortfolioId.newId();
    Portfolio p1 = mock(Portfolio.class);
    when(p1.getDisplayCurrency()).thenReturn(Currency.USD);
    when(portfolioLoader.loadUserPortfolio(pid, userId)).thenReturn(p1);

    // Act
    service.computePortfolioValuation(pid, userId);

    // Assert: Verify USD was used because the portfolio is set to USD
    verify(portfolioValuationService).calculatePortfolioValuation(eq(p1), eq(Currency.USD),
        anyMap());
  }

  @Test
  @DisplayName("Returns empty ValuationView when summary requested for user with no portfolios")
  void summaryReturnsEmptyViewWhenEmpty() {
    Currency mockBaseCurrency = Currency.of("CAD");
    UserPreferences mockPrefs = new UserPreferences(userId, mockBaseCurrency, false, false, null);
    when(portfolioLoader.loadAllUserPortfolios(userId)).thenReturn(List.of());
    when(userPreferencesService.get(userId)).thenReturn(mockPrefs);

    ValuationView result = service.computeSummaryValuation(userId);

    assertNotNull(result);
    assertEquals(mockBaseCurrency, result.displayCurrency());
    assertEquals(BigDecimal.ZERO, result.gainLossPercent());
    assertEquals(Money.zero(mockBaseCurrency), result.totalValue());
    assertEquals(Money.zero(mockBaseCurrency), result.totalCashBalance());
    assertFalse(result.hasStaleData());
    assertNotNull(result.asOfDate()); // Ensures it captured Instant.now()
  }
}