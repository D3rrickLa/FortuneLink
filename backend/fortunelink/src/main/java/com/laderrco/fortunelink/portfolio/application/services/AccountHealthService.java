package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountHealthService {
  private static final Logger log = LoggerFactory.getLogger(AccountHealthService.class);
  private final PortfolioRepository portfolioRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW) // own transaction, always commits
  public void markStale(AccountId accountId) {
    try {
      portfolioRepository.markAccountStale(accountId);
      log.info("Account {} marked as STALE due to calculation failure.", accountId);
    } catch (Exception e) {
      log.error("Failed to mark account {} as stale", accountId, e);
    }
  }
}
