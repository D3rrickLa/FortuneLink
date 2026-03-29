package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioLifecycleService {
  private static final String DEFAULT_NAME = "Default Account";

  private final PortfolioRepository portfolioRepository;
  private final PortfolioViewMapper portfolioViewMapper;


  private final PortfolioLifecycleCommandValidator validator;
  private final TransactionTemplate transactionTemplate;
  private final PortfolioLoader portfolioLoader;

  public PortfolioView createPortfolio(CreatePortfolioCommand command) {
    ValidationUtils.validate(command, validator::validate, "createPortfolio");

    Portfolio portfolio = Portfolio.createNew(command.userId(), command.name(),
        command.description(), command.currency());

    if (command.createDefaultAccount()) {
      portfolio.createAccount(DEFAULT_NAME, command.defaultAccountType(), command.currency(),
          command.defaultStrategy());
    }

    Portfolio savedPortfolio;
    try {
      savedPortfolio = portfolioRepository.save(portfolio);
    } catch (DataIntegrityViolationException e) {
      throw new PortfolioLimitReachedException("Portfolio already exists for this user");
    }
    return portfolioViewMapper.toNewPortfolioView(savedPortfolio);
  }

  /**
   * NOTE: lifecycle shouldn't call MarketDataService. Returns only what changed, portfolio
   * metadata. Callers that need a full view should query PortfolioQueryService afterward. This is
   * intentional: lifecycle services should not own read-side concerns.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public PortfolioView updatePortfolio(UpdatePortfolioCommand command) {
    ValidationUtils.validate(command, validator::validate, "updatePortfolio");

    Portfolio saved = transactionTemplate.execute(status -> {
      Portfolio existing = portfolioLoader.loadUserPortfolioWithGraph(command.portfolioId(),
          command.userId());
      existing.updateDetails(command.name(), command.description());
      existing.updateDisplayCurrency(command.currency());
      return portfolioRepository.save(existing);
    });

    if (saved == null) {
      throw new IllegalStateException("Transaction failed for: " + command.portfolioId());
    }

    // Return a lightweight view, no market data, no fees.
    // If the caller needs a full portfolio view, they call PortfolioQueryService.
    return portfolioViewMapper.toNewPortfolioView(saved);
  }

  public void deletePortfolio(DeletePortfolioCommand command) {
    ValidationUtils.validate(command, validator::validate, "deletePortfolio");

    Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(),
        command.userId()).orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

    if (command.softDelete()) {
      softDelete(portfolio, command);
    } else {
      portfolioRepository.delete(command.portfolioId());
    }
  }

  private void softDelete(Portfolio portfolio, DeletePortfolioCommand command) {
    try {
      if (command.recursive()) {
        closeAllEligibleAccounts(portfolio);
      }
      portfolio.markAsDeleted(command.userId());
      portfolioRepository.save(portfolio);
    } catch (PortfolioDeletionException e) {
      throw e;
    } catch (PortfolioNotEmptyException e) {
      throw new PortfolioDeletionException(
          "Cannot delete portfolio: close accounts first or use recursive delete.");
    } catch (PortfolioAlreadyDeletedException | IllegalStateException e) {
      throw new PortfolioDeletionException(e.getMessage());
    }
  }

  private void closeAllEligibleAccounts(Portfolio portfolio) {
    boolean hasNonEmptyAccounts = portfolio.getAccounts().stream().filter(Account::isActive)
        .anyMatch(acc -> acc.getPositionCount() > 0 || acc.getCashBalance().isPositive());

    if (hasNonEmptyAccounts) {
      throw new PortfolioDeletionException(
          "Recursive delete requires all accounts to have zero positions and zero cash balance.");
    }

    portfolio.getAccounts().stream().filter(Account::isActive)
        .forEach(acc -> portfolio.closeAccount(acc.getAccountId()));
  }
}
