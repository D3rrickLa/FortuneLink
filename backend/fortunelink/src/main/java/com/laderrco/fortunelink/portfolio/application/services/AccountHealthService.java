package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
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
    public void markStale(PortfolioId portfolioId, UserId userId, AccountId accountId) {
        try {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
            portfolio.reportRecalculationFailure(accountId);
            portfolioRepository.save(portfolio);
        } catch (Exception e) {
            log.error("Failed to mark account as stale - manual intervention required.");
        }
    }
}
