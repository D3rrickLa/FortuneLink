package com.laderrco.fortunelink.portfolio.application.utils;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PortfolioLoader {
  private final PortfolioRepository portfolioRepository;

  public PortfolioLoader(PortfolioRepository portfolioRepository) {
    this.portfolioRepository = portfolioRepository;
  }

  public Portfolio loadUserPortfolio(PortfolioId portfolioId, UserId userId) {
    Portfolio portfolio = portfolioRepository
        .findByIdAndUserId(portfolioId, userId)
        .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

    if (portfolio.isDeleted()) {
      throw new PortfolioNotFoundException(portfolioId);
    }

    return portfolio;
  }

  /**
   * Lightweight ownership check, does not load the Portfolio aggregate. Use existsByIdAndUserId
   * wherever you only need to verify access without needing to mutate or read the portfolio
   * itself.
   */
  public void validateOwnership(PortfolioId portfolioId, UserId userId) {
    if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
      throw new PortfolioNotFoundException(portfolioId);
    }
  }

  /**
   * Loads all active portfolios for a user. Mirrors loadUserPortfolio() semantics: deleted
   * portfolios are excluded at the query level. Returns empty list (not an exception) when the user
   * has no active portfolios — that's a valid state, not an error.
   */
  public List<Portfolio> loadAllUserPortfolios(UserId userId) {
    return portfolioRepository.findAllActiveByUserId(userId);
  }
}
