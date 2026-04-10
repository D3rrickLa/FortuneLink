package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountHealthServiceTest {
  @Mock
  private PortfolioRepository portfolioRepository;

  @InjectMocks
  private AccountHealthService accountHealthService;

  @Test
  @DisplayName("markStale: successfully calls repository and logs info")
  void markStaleSuccess() {
    AccountId accountId = AccountId.newId();
    LogCaptor logCaptor = LogCaptor.forClass(AccountHealthService.class);

    // Act
    accountHealthService.markStale(accountId);

    // Assert
    verify(portfolioRepository).markAccountStale(accountId);
    assertThat(logCaptor.getInfoLogs())
        .contains("Account " + accountId + " marked as STALE due to calculation failure.");
  }

  @Test
  @DisplayName("markStale: logs error when repository throws exception")
  void markStaleExceptionLogsError() {
    AccountId accountId = AccountId.newId();
    LogCaptor logCaptor = LogCaptor.forClass(AccountHealthService.class);

    // Arrange: Simulate a database failure
    doThrow(new RuntimeException("DB Error"))
        .when(portfolioRepository).markAccountStale(accountId);

    // Act
    accountHealthService.markStale(accountId);

    // Assert
    assertThat(logCaptor.getErrorLogs())
        .hasSize(1)
        .anyMatch(log -> log.contains("Failed to mark account " + accountId + " as stale"));
  }
}