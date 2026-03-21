package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;

import nl.altindag.log.LogCaptor;

@ExtendWith(MockitoExtension.class)
public class AccountHealthServiceTest {
  @Mock
  private PortfolioRepository portfolioRepository;

  @InjectMocks
  private AccountHealthService accountHealthService;

  @Test
  void testMarkStale_success_whenMarkingAccountAsStale() {
    UserId userId = UserId.random();
    PortfolioId portfolioId = PortfolioId.newId();
    AccountId accountId = AccountId.newId();

    Portfolio mockPortfolio = mock(Portfolio.class);

    when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(mockPortfolio));
    when(portfolioRepository.save(mockPortfolio)).thenReturn(mockPortfolio);

    assertDoesNotThrow(() -> accountHealthService.markStale(portfolioId, userId, accountId));
    verify(mockPortfolio).reportRecalculationFailure(accountId);
    verify(portfolioRepository).save(mockPortfolio);
  }

  @Test
  void testMarkStale_failure_whenMarkingAccountAsStaleAccountNotFouns() {
    LogCaptor logCaptor = LogCaptor.forClass(AccountHealthService.class);
    UserId userId = UserId.random();
    PortfolioId portfolioId = PortfolioId.newId();
    AccountId accountId = AccountId.newId();

    when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.empty());
    assertDoesNotThrow(() -> accountHealthService.markStale(portfolioId, userId, accountId));
    assertThat(logCaptor.getErrorLogs())
      .hasSize(1)
      .anyMatch(log -> log.contains("Failed to mark account as stale"));

    verify(portfolioRepository, never()).save(any());
  }

}