package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import java.util.Optional;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Health Service Operations")
class AccountHealthServiceTest {

  @Mock
  private PortfolioRepository portfolioRepository;

  @InjectMocks
  private AccountHealthService accountHealthService;

  @Test
  @DisplayName("markStale: successfully updates portfolio when account is found")
  void markStaleValidAccountUpdatesPortfolioSuccess() {
    UserId userId = UserId.random();
    PortfolioId portfolioId = PortfolioId.newId();
    AccountId accountId = AccountId.newId();
    Portfolio mockPortfolio = mock(Portfolio.class);

    when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(
        Optional.of(mockPortfolio));
    when(portfolioRepository.save(mockPortfolio)).thenReturn(mockPortfolio);

    assertDoesNotThrow(() -> accountHealthService.markStale(accountId));

    verify(mockPortfolio).reportRecalculationFailure(accountId);
    verify(portfolioRepository).save(mockPortfolio);
  }

  @Test
  @DisplayName("markStale: logs error and skips save when portfolio is not found")
  void markStaleAccountNotFoundLogsError() {
    LogCaptor logCaptor = LogCaptor.forClass(AccountHealthService.class);
    UserId userId = UserId.random();
    PortfolioId portfolioId = PortfolioId.newId();
    AccountId accountId = AccountId.newId();

    when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> accountHealthService.markStale(accountId));

    assertThat(logCaptor.getErrorLogs()).hasSize(1)
        .anyMatch(log -> log.contains("Failed to mark account as stale"));

    verify(portfolioRepository, never()).save(any());
  }
}